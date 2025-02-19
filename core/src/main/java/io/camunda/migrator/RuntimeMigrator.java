package io.camunda.migrator;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.camunda.client.api.command.DeployResourceCommandStep1.DeployResourceCommandStep2;

@Component
public class RuntimeMigrator {

  @Autowired
  protected RuntimeService runtimeService;

  protected CamundaClient camundaClient = CamundaClient.newClientBuilder().usePlaintext().build();

  public void migrate() {
    // Deploy process
    var deployResource = camundaClient.newDeployResourceCommand();

    List<Path> models = findResourceFilesWithExtension("bpmn");

    DeployResourceCommandStep2 deployResourceCommandStep2 = null;
    for (Path model : models) {
      deployResourceCommandStep2 = deployResource.addResourceFile(model.toFile().getName());
    }

    if (deployResourceCommandStep2 != null) {
      deployResourceCommandStep2.send().join();
    }

    runtimeService.createProcessInstanceQuery().rootProcessInstances().list().forEach(processInstance -> {
      String c7ProcessInstanceId = processInstance.getProcessInstanceId();

    Map<String, Object> globalVariables = new HashMap<>();

    runtimeService.createVariableInstanceQuery()
        .activityInstanceIdIn(c7ProcessInstanceId)
        .list()
        .forEach(variable -> globalVariables.put(variable.getName(), variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.

    globalVariables.put("legacyId", c7ProcessInstanceId);

    camundaClient.newCreateInstanceCommand()
        .bpmnProcessId(processInstance.getProcessDefinitionKey())
        .latestVersion()
        .variables(globalVariables) // process instance global variables
        .send()
        .join();

    camundaClient.newActivateJobsCommand()
        .jobType("migrator")
        .maxJobsToActivate(Integer.MAX_VALUE)
        .timeout(Duration.ofMinutes(1))
        .send()
        .join()
        .getJobs().forEach(activatedJob -> {

          String legacyId = (String) activatedJob.getVariable("legacyId");

          ModifyProcessInstanceCommandStep1 modifyProcessInstance = camundaClient.newModifyProcessInstanceCommand(
                  activatedJob.getProcessInstanceKey())
              // Cancel start event instance where migrator job sits to avoid executing the activities twice.
              .terminateElement(activatedJob.getElementInstanceKey()).and();

          ModifyProcessInstanceCommandStep3 modifyInstructions = null;
          ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(legacyId);
          Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree, new HashMap<>());

          removeMultiInstances(activityInstanceMap);

          for (String activityInstanceId : activityInstanceMap.keySet()) {
            ActInstance actInstance = activityInstanceMap.get(activityInstanceId);
            String activityId = actInstance.id().split("#multiInstanceBody")[0];

            Map<String, Object> localVariables = new HashMap<>();

            runtimeService.createVariableInstanceQuery()
                .activityInstanceIdIn(activityInstanceId)
                .list()
                .forEach(variable -> localVariables.put(variable.getName(), variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.

            String subProcessInstanceId = actInstance.subProcessInstanceId();
            if (subProcessInstanceId != null) {
              localVariables.put("legacyId", subProcessInstanceId);
            }
            modifyInstructions = modifyProcessInstance.activateElement(activityId).withVariables(localVariables, activityId);
          }
          modifyInstructions.send().join();
          // no need to complete the job since the modification canceled the migrator job in the start event
        });
    });
  }

  /**
   * This removes multi-instances from the active activity instance map and only keeps the #multiInstanceBody activity.
   * Like this, the C8 process model takes care of instantiating X instances.
   * TODO: how would this work for multi-instance call activities?
   */
  protected void removeMultiInstances(Map<String, ActInstance> activityInstanceMap) {
    Set<String> multiInstanceBodies = activityInstanceMap.values()
        .stream()
        .map(ActInstance::id)
        .filter(activityId -> activityId.endsWith("#multiInstanceBody"))
        .map(activityId -> activityId.substring(0, activityId.length() - "#multiInstanceBody".length()))
        .collect(Collectors.toSet());

    activityInstanceMap.entrySet().removeIf(entry -> multiInstanceBodies.contains(entry.getValue().id()));
  }

  public Map<String, ActInstance> getActiveActivityIdsById(ActivityInstance activityInstance, Map<String, ActInstance> activeActivities) {
    Arrays.asList(activityInstance.getChildActivityInstances()).forEach(actInst -> {
      activeActivities.putAll(getActiveActivityIdsById(actInst, activeActivities));

      if (!"subProcess".equals(actInst.getActivityType())) {
        // TODO throw unsupported exception on multi-instance
        activeActivities.put(actInst.getId(), new ActInstance(actInst.getActivityId(), ((ActivityInstanceImpl) actInst).getSubProcessInstanceId()));
      }
    });

    /* TODO: Transition instances might map to start before or after.
    When it maps to asyncBefore it should be fine. When it maps to asyncAfter an execution is fired twice in C7 and C8.
     */
    Arrays.asList(activityInstance.getChildTransitionInstances()).forEach(ti -> {
      var transitionInstance = ((TransitionInstanceImpl) ti);
      if (!"subProcess".equals(transitionInstance.getActivityType())) {
        // TODO throw unsupported exception on multi-instance
        activeActivities.put(transitionInstance.getId(), new ActInstance(transitionInstance.getActivityId(), transitionInstance.getSubProcessInstanceId()));
      }
    });
    return activeActivities;
  }

  record ActInstance(String id, String subProcessInstanceId) {
  }



  public static List<Path> findResourceFilesWithExtension(String extension)  {
    List<Path> result = new ArrayList<>();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Enumeration<URL> resources = null;
    try {
      resources = classLoader.getResources("");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      if (resource.getProtocol().equals("file")) {
        File directory = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8));
        if (directory.isDirectory()) {
          Path resourceFolder = Paths.get(directory.getPath(), "resources");
          if (Files.exists(resourceFolder) && Files.isDirectory(resourceFolder)) {
            result.addAll(findFilesInDirectory(resourceFolder.toFile(), extension));
          }
        }
      }
    }
    return result;
  }

  public static List<Path> findFilesInDirectory(File directory, String extension) {
    List<Path> result = new ArrayList<>();
    try {
      Files.walk(Paths.get(directory.toURI()))
          .filter(path -> path.toString().endsWith("." + extension))
          .forEach(result::add);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

}
