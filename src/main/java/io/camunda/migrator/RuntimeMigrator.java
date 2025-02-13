package io.camunda.migrator;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RuntimeMigrator {

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected CamundaClient camundaClient;

  public RuntimeMigrator(RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService, CamundaClient camundaClient) {
    this.repositoryService = repositoryService;
    this.runtimeService = runtimeService;
    this.taskService = taskService;
    this.camundaClient = camundaClient;
  }

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
          Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(legacyId);
          for (String activityInstanceId : activityInstanceMap.keySet()) {
            ActInstance actInstance = activityInstanceMap.get(activityInstanceId);
            String activityId = actInstance.id();
            Map<String, Object> localVariables = runtimeService.createVariableInstanceQuery()
                .activityInstanceIdIn(activityInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(VariableInstance::getName, VariableInstance::getValue));
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

  public Map<String, ActInstance> getActiveActivityIdsById(String processInstanceId) {
    Set<String> activityIds = new HashSet<>(runtimeService.getActiveActivityIds(processInstanceId));

    ActivityInstance processInstance = runtimeService.getActivityInstance(processInstanceId);

    Map<String, ActInstance> activeActivities = new HashMap<>();
    activityIds.forEach(activityId -> {
      Arrays.asList(processInstance.getActivityInstances(activityId)).forEach(activityInstance -> {
        activeActivities.put(activityInstance.getId(), new ActInstance(activityInstance.getActivityId(), ((ActivityInstanceImpl)activityInstance).getSubProcessInstanceId()));
      });

      // TODO: Transition instances might map to start before or after. Does this matter? If not, fall back to RuntimeService#getActiveActivityIds
      Arrays.asList(processInstance.getTransitionInstances(activityId)).forEach(transitionInstance -> {
        activeActivities.put(transitionInstance.getId(), new ActInstance(transitionInstance.getActivityId(), ((TransitionInstanceImpl)transitionInstance).getSubProcessInstanceId()));
      });
    });

    return activeActivities;
  }

  record ActInstance(String id, String subProcessInstanceId) {
  }

}
