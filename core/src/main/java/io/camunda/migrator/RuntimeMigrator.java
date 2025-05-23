/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.migrator.history.IdKeyDbModel;
import io.camunda.migrator.history.IdKeyMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.camunda.migrator.HistoryMigrator.BATCH_SIZE;

@Component
public class RuntimeMigrator {

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  private IdKeyMapper idKeyMapper;

  @Autowired
  protected CamundaClient camundaClient;

  public void migrate() {
    String latestLegacyId = idKeyMapper.findLatestIdByType("runtimeProcessInstance");
    ProcessInstanceQuery processInstanceQuery = ((ProcessInstanceQueryImpl) runtimeService.createProcessInstanceQuery())
        .idAfter(latestLegacyId)
        .rootProcessInstances();

    long maxLegacyProcessInstanceCount = processInstanceQuery.count();
    for (int i = 0; i < maxLegacyProcessInstanceCount; i = i + BATCH_SIZE - 1) {
      processInstanceQuery.listPage(i, BATCH_SIZE).forEach(legacyProcessInstance -> {
        String legacyId = legacyProcessInstance.getId();
        var idKeyDbModel = new IdKeyDbModel();
        idKeyDbModel.setId(legacyId);
        idKeyDbModel.setKey(null);
        idKeyDbModel.setType("runtimeProcessInstance");
        idKeyMapper.insert(idKeyDbModel);
      });
    }

    // TODO: paginate this
    idKeyMapper.findProcessInstanceIds().forEach(legacyProcessInstanceId -> {
      Map<String, Object> globalVariables = new HashMap<>();

      runtimeService.createVariableInstanceQuery()
          .activityInstanceIdIn(legacyProcessInstanceId)
          .list()
          .forEach(variable -> globalVariables.put(variable.getName(), variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.

      globalVariables.put("legacyId", legacyProcessInstanceId);

      String bpmnProcessId = runtimeService.createProcessInstanceQuery()
          .processInstanceId(legacyProcessInstanceId)
          .singleResult()
          .getProcessDefinitionKey();

      long processInstanceKey = camundaClient.newCreateInstanceCommand()
          .bpmnProcessId(bpmnProcessId)
          .latestVersion()
          .variables(globalVariables) // process instance global variables
          .send()
          .join()
          .getProcessInstanceKey();

      var keyIdDbModel = new IdKeyDbModel();
      keyIdDbModel.setId(legacyProcessInstanceId);
      keyIdDbModel.setKey(processInstanceKey);
      idKeyMapper.updateKeyById(keyIdDbModel);

      List<ActivatedJob> migratorJobs = null;
      do {
        migratorJobs = camundaClient.newActivateJobsCommand()
            .jobType("migrator")
            // TODO: review #maxJobsToActivate and #timeout
            .maxJobsToActivate(Integer.MAX_VALUE)
            .timeout(Duration.ofMinutes(1))
            .send()
            .join()
            .getJobs();
            migratorJobs.forEach(activatedJob -> {

              String legacyId = (String) activatedJob.getVariable("legacyId");

              ModifyProcessInstanceCommandStep1 modifyProcessInstance = camundaClient.newModifyProcessInstanceCommand(
                      activatedJob.getProcessInstanceKey())
                  // Cancel start event instance where migrator job sits to avoid executing the activities twice.
                  .terminateElement(activatedJob.getElementInstanceKey()).and();

              ModifyProcessInstanceCommandStep3 modifyInstructions = null;
              ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(legacyId);
              Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree,
                  new HashMap<>());

              for (String activityInstanceId : activityInstanceMap.keySet()) {
                ActInstance actInstance = activityInstanceMap.get(activityInstanceId);
                String activityId = actInstance.id();

                Map<String, Object> localVariables = new HashMap<>();

                runtimeService.createVariableInstanceQuery()
                    .activityInstanceIdIn(activityInstanceId)
                    .list()
                    .forEach(variable -> localVariables.put(variable.getName(),
                        variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.

                String subProcessInstanceId = actInstance.subProcessInstanceId();
                if (subProcessInstanceId != null) {
                  localVariables.put("legacyId", subProcessInstanceId);
                }
                modifyInstructions = modifyProcessInstance.activateElement(activityId)
                    .withVariables(localVariables, activityId);
              }
              modifyInstructions.send().join();
              // no need to complete the job since the modification canceled the migrator job in the start event
            });
      } while (!migratorJobs.isEmpty());
    });
  }

  public Map<String, ActInstance> getActiveActivityIdsById(ActivityInstance activityInstance, Map<String, ActInstance> activeActivities) {
    Arrays.asList(activityInstance.getChildActivityInstances()).forEach(actInst -> {
      activeActivities.putAll(getActiveActivityIdsById(actInst, activeActivities));

      if (!"subProcess".equals(actInst.getActivityType())) {
        // TODO skip migration of process instance when multi-instance is active
        activeActivities.put(actInst.getId(), new ActInstance(actInst.getActivityId(), ((ActivityInstanceImpl) actInst).getSubProcessInstanceId()));
      }
    });

    /* TODO: Transition instances might map to start before or after.
    When it maps to asyncBefore it should be fine. When it maps to asyncAfter an execution is fired twice in C7 and C8.
     */
    Arrays.asList(activityInstance.getChildTransitionInstances()).forEach(ti -> {
      var transitionInstance = ((TransitionInstanceImpl) ti);
      if (!"subProcess".equals(transitionInstance.getActivityType())) {
        // TODO skip migration of process instance when multi-instance is active
        activeActivities.put(transitionInstance.getId(), new ActInstance(transitionInstance.getActivityId(), transitionInstance.getSubProcessInstanceId()));
      }
    });
    return activeActivities;
  }

  record ActInstance(String id, String subProcessInstanceId) {
  }

}
