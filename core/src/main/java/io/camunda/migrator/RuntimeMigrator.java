/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.migrator.history.IdKeyDbModel;
import io.camunda.migrator.history.IdKeyMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuntimeMigrator {

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected IdKeyMapper idKeyMapper;

  @Autowired
  protected CamundaClient camundaClient;

  protected boolean retryMode = false;

  public void migrate() {
    List<String> processInstanceIds = fetchProcessInstancesToMigrate();

    processInstanceIds.forEach(legacyProcessInstanceId -> {
      if (validateProcessInstanceMigration(legacyProcessInstanceId)) {
        long processInstanceKey = startNewProcessInstance(legacyProcessInstanceId);
        insertKeyForRuntimeProcessInstanceEntity(legacyProcessInstanceId, processInstanceKey);
      } else {
        System.out.println("Skipping process instance with legacyId " + legacyProcessInstanceId); // TODO log
        insertKeyForRuntimeProcessInstanceEntity(legacyProcessInstanceId, null);
      }
    });

    activateMigratorJobs();
  }

  protected List<String> fetchProcessInstancesToMigrate() {
    List<String> processInstanceIds;
    try {
      if (retryMode) {
        processInstanceIds = idKeyMapper.findSkippedProcessInstanceIds();
      } else {
        String latestLegacyId = idKeyMapper.findLatestIdByType("runtimeProcessInstance");
        processInstanceIds = ((ProcessInstanceQueryImpl) runtimeService.createProcessInstanceQuery())
            .idAfter(latestLegacyId)
            .rootProcessInstances()
            .orderByProcessInstanceId()
            .asc()
            .list()
            .stream()
            .map(Execution::getId).toList();
      }
    } catch(PersistenceException | ProcessEngineException e) {
      System.out.println("An error occurred while fetching instances to migrate, the migration will halt"); // TODO log
      throw new MigratorException("Error while fetching instances to migrate", e);
    }
    return processInstanceIds;
  }

  protected long startNewProcessInstance(String legacyProcessInstanceId) {
    try {
      Map<String, Object> globalVariables = generateGlobalVariables(legacyProcessInstanceId);
      String bpmnProcessId = runtimeService.createProcessInstanceQuery().processInstanceId(legacyProcessInstanceId).singleResult().getProcessDefinitionKey();
      return camundaClient.newCreateInstanceCommand()
          .bpmnProcessId(bpmnProcessId)
          .latestVersion()
          .variables(globalVariables)
          .send()
          .join()
          .getProcessInstanceKey();
    } catch (ProcessEngineException | ClientException e) {
      System.out.println("An error occurred while starting new process instance with legacyId " + legacyProcessInstanceId + ", the migration will halt"); // TODO log
      throw new MigratorException("Error while migrating process instance with legacyId " + legacyProcessInstanceId, e);
    }
  }

  protected void insertKeyForRuntimeProcessInstanceEntity(String legacyProcessInstanceId, Long processInstanceKey) {
    var keyIdDbModel = new IdKeyDbModel();
    keyIdDbModel.setId(legacyProcessInstanceId);
    keyIdDbModel.setKey(processInstanceKey);
    keyIdDbModel.setType("runtimeProcessInstance"); // Unused for update, necessary for insert
    try {
      if (retryMode) {
        idKeyMapper.updateKeyById(keyIdDbModel);
      } else {
        idKeyMapper.insert(keyIdDbModel);
      }
    } catch (PersistenceException e) {
      System.out.println("An error occurred while inserting or updating runtimeProcessInstance entity with id " + legacyProcessInstanceId + " in the database, the migration will halt"); // TODO log
      throw new MigratorException("Error while inserting or updating runtimeProcessInstance entity with id " + legacyProcessInstanceId, e);
    }
  }

  protected Map<String, Object> generateGlobalVariables(String legacyProcessInstanceId) {
    Map<String, Object> globalVariables = new HashMap<>();
    runtimeService.createVariableInstanceQuery()
        .activityInstanceIdIn(legacyProcessInstanceId)
        .list()
        .forEach(variable -> globalVariables.put(variable.getName(), variable.getValue())); // Collectors#toMap cannot handle null values and throws NPE.
    globalVariables.put("legacyId", legacyProcessInstanceId);
    return globalVariables;
  }

  /**
   * This method iterates over all the activity instances of the root process instance and its
   * children until it either finds an activityInstance that cannot be migrated or the iteration ends.
   * For now, only multi-instance activities will fail validation.
   * @param legacyProcessInstanceId the legacy id of the root process instance.
   * @return true if all of the process hierarchy can be migrated, false otherwise.
   */
  protected boolean validateProcessInstanceMigration(String legacyProcessInstanceId) {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().rootProcessInstanceId(legacyProcessInstanceId).list();

    for (ProcessInstance processInstance : processInstances) {
      ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstance.getId());
      Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree, new HashMap<>());
      BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processInstance.getProcessDefinitionId());

      for (ActInstance actInstance : activityInstanceMap.values()) {
        FlowElement element = bpmnModelInstance.getModelElementById(actInstance.activityId());
        if ((element instanceof Activity activity) && (activity.getLoopCharacteristics() instanceof MultiInstanceLoopCharacteristics)) {
          return false;
        }
      }
     }
    return true;
  }

  protected void activateMigratorJobs() {
    List<ActivatedJob> migratorJobs;
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
          String activityId = actInstance.activityId();

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

  record ActInstance(String activityId, String subProcessInstanceId) {
  }

  public void setRetryMode(boolean retryMode) {
    this.retryMode = retryMode;
  }
}
