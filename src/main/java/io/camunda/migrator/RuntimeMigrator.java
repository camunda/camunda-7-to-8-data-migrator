package io.camunda.migrator;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RuntimeMigrator {

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected RuntimeService runtimeService;

  protected CamundaClient camundaClient = CamundaClient.newClientBuilder().usePlaintext().build();

  public void migrate() {
    // Callback -> when PI started, get activeActivityIds of this PI. Doesn't matter if parent or subprocess instance.

    // Deploy C7 process
    /*repositoryService.createDeployment()
        .addClasspathResource("test-processes/complex-process-c7.bpmn")
        .addClasspathResource("test-processes/complex-child-process-c7.bpmn")
        .deploy();

    // Generate C7 data
    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("complex-process", Variables.putValue("myGlobalVar", 1234)).getId();
    taskService.createTaskQuery().processInstanceId(c7ProcessInstanceId).list().stream().map(Task::getId).toList()
        .forEach(taskService::complete);
    String generatedDataLegacyChildId = runtimeService.createProcessInstanceQuery().superProcessInstanceId(c7ProcessInstanceId).singleResult().getId();
    taskService.createTaskQuery().processInstanceId(generatedDataLegacyChildId).list().stream().map(Task::getId).toList()
        .forEach(taskService::complete);*/

    repositoryService.createDeployment()
        .addClasspathResource("test-processes/target-complex-process-c7.bpmn")
        .addClasspathResource("test-processes/complex-child-process-c7.bpmn")
        .deploy();

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("cornercasesProcess", Variables.putValue("myGlobalVar", 1234)).getId();

    // Migrate to C8
    // Deploy process
    long processDefinitionKey = camundaClient.newDeployResourceCommand()
        //.addResourceStream(getClass().getClassLoader().getResourceAsStream("test-processes/complex-process-c8.bpmn"), "complex-process-c8.bpmn")
        //.addResourceStream(getClass().getClassLoader().getResourceAsStream("test-processes/complex-child-process-c8.bpmn"), "complex-child-process-c8.bpmn")
        .addResourceFromClasspath("test-processes/target-complex-process-c8.bpmn")
        .addResourceFromClasspath("test-processes/complex-child-process-c8.bpmn")
        .send()
        .join()
        .getProcesses()
        .get(0)
        .getProcessDefinitionKey();

    Map<String, Object> globalVariables = new HashMap<>();

    runtimeService.createVariableInstanceQuery()
        .activityInstanceIdIn(c7ProcessInstanceId)
        .list()
        .forEach(variable -> globalVariables.put(variable.getName(), variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.

    globalVariables.put("legacyId", c7ProcessInstanceId);

    camundaClient.newCreateInstanceCommand()
        .processDefinitionKey(processDefinitionKey)
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
          ActivityInstance processInstance = runtimeService.getActivityInstance(legacyId);
          Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(processInstance, new HashMap<>());

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

}
