/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.history.IdKeyMapper.TYPE;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.migrator.history.IdKeyDbModel;
import io.camunda.migrator.history.IdKeyMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.ibatis.exceptions.PersistenceException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.runtime.VariableInstanceQuery;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuntimeMigrator {

  protected static final Logger LOGGER = LoggerFactory.getLogger(RuntimeMigrator.class);

  public final static int MAX_BATCH_SIZE = 500;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected IdKeyMapper idKeyMapper;

  @Autowired
  protected CamundaClient camundaClient;

  protected boolean retryMode = false;

  protected int batchSize = MAX_BATCH_SIZE;

  public void migrate() {
    fetchProcessInstancesToMigrate(legacyProcessInstanceId -> {
      if (skipProcessInstance(legacyProcessInstanceId)) {
        LOGGER.info("Skipping process instance with legacyId: {}", legacyProcessInstanceId);
        storeMapping(legacyProcessInstanceId, null);

      } else {
        long processInstanceKey = startNewProcessInstance(legacyProcessInstanceId);
        storeMapping(legacyProcessInstanceId, processInstanceKey);

      }
    });

    activateMigratorJobs();
  }

  protected boolean skipProcessInstance(String legacyProcessInstanceId) {
    try {
      validateProcessInstanceState(legacyProcessInstanceId);
    } catch (IllegalStateException e) {
      LOGGER.warn(e.getMessage());
      return true;
    }

    return false;
  }

  protected void fetchProcessInstancesToMigrate(Consumer<String> storeMappingConsumer) {
    try {
      if (retryMode) {
        long maxResultsCount = idKeyMapper.findSkippedProcessInstanceIdsCount();
        // Hardcode offset to 0 since each callback updates the database and leads to fresh results.
        paginate(maxResultsCount, i -> idKeyMapper.findSkippedProcessInstanceIds(0, batchSize), storeMappingConsumer);

      } else {
        String latestLegacyId = idKeyMapper.findLatestIdByType(TYPE.RUNTIME_PROCESS_INSTANCE);

        ProcessInstanceQuery processInstanceQuery = ((ProcessInstanceQueryImpl) runtimeService.createProcessInstanceQuery())
            .idAfter(latestLegacyId)
            .rootProcessInstances()
            .orderByProcessInstanceId()
            .asc();

        long maxResultsCount = processInstanceQuery.count();
        paginate(maxResultsCount, i -> processInstanceQuery.listPage(i, batchSize)
            .stream()
            .map(ProcessInstance::getId)
            .collect(Collectors.toSet()), storeMappingConsumer);
      }
    } catch (PersistenceException | ProcessEngineException e) {
      LOGGER.error("An error occurred while fetching instances to migrate, the migration will halt");
      throw new MigratorException("Error while fetching instances to migrate", e);
    }
  }

  protected void storeMapping(String legacyProcessInstanceId, Long processInstanceKey) {
    var keyIdDbModel = new IdKeyDbModel();
    keyIdDbModel.setId(legacyProcessInstanceId);
    keyIdDbModel.setKey(processInstanceKey);
    keyIdDbModel.setType(TYPE.RUNTIME_PROCESS_INSTANCE);

    try {
      if (retryMode) {
        idKeyMapper.updateKeyById(keyIdDbModel);
      } else {
        idKeyMapper.insert(keyIdDbModel);
      }
    } catch (PersistenceException e) {
      LOGGER.error("An error occurred while inserting runtimeProcessInstance entity with id {} in the database, the migration will halt", legacyProcessInstanceId);
      throw new MigratorException("Error while inserting runtimeProcessInstance entity with id " + legacyProcessInstanceId, e);
    }
  }

  private long startNewProcessInstance(String legacyProcessInstanceId) {
    try {
      Map<String, Object> globalVariables = getGlobalVariables(legacyProcessInstanceId);

      String bpmnProcessId = runtimeService.createProcessInstanceQuery()
          .processInstanceId(legacyProcessInstanceId)
          .singleResult()
          .getProcessDefinitionKey();

      return camundaClient.newCreateInstanceCommand()
          .bpmnProcessId(bpmnProcessId)
          .latestVersion()
          .variables(globalVariables)
          .send()
          .join()
          .getProcessInstanceKey();
    } catch (ProcessEngineException | ClientException e) {
      LOGGER.error("An error occurred while starting new process instance with legacyId {}, the migration will halt", legacyProcessInstanceId);
      throw new MigratorException("Error while migrating process instance with legacyId " + legacyProcessInstanceId, e);
    }
  }

  protected Map<String, Object> getGlobalVariables(String legacyProcessInstanceId) {
    Map<String, Object> globalVariables = new HashMap<>();

    VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery()
        .activityInstanceIdIn(legacyProcessInstanceId);

    long maxVariablesCount = variableQuery.count();
    paginate(maxVariablesCount, i -> new HashSet<>(variableQuery.listPage(i, batchSize)),
        var -> globalVariables.put(var.getName(), var.getValue()));

    globalVariables.put("legacyId", legacyProcessInstanceId);
    return globalVariables;
  }

  /**
   * This method iterates over all the activity instances of the root process instance and its
   * children until it either finds an activityInstance that cannot be migrated or the iteration ends.
   * For now, only multi-instance activities will fail validation.
   * @param legacyProcessInstanceId the legacy id of the root process instance.
   */
  protected void validateProcessInstanceState(String legacyProcessInstanceId) {
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
        .rootProcessInstanceId(legacyProcessInstanceId);

    long maxProcessInstancesCount = processInstanceQuery.count();
    paginate(maxProcessInstancesCount, i -> new HashSet<>(processInstanceQuery.list()), processInstance -> {
      ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstance.getId());
      Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree, new HashMap<>());
      BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processInstance.getProcessDefinitionId());

      for (ActInstance actInstance : activityInstanceMap.values()) {
        FlowElement element = bpmnModelInstance.getModelElementById(actInstance.activityId());
        if ((element instanceof Activity activity) && (activity.getLoopCharacteristics() instanceof MultiInstanceLoopCharacteristics)) {
          throw new IllegalStateException("Found multi-instance loop characteristics for " + element.getName() +
              " in C7 process instance " + processInstance.getId() + ".");
        }
      }
    });
  }

  protected void activateMigratorJobs() {
    List<ActivatedJob> migratorJobs = null;
    do {
      migratorJobs = camundaClient.newActivateJobsCommand()
          .jobType("migrator")
          .maxJobsToActivate(batchSize)
          .send()
          .join()
          .getJobs();

      if (migratorJobs.isEmpty()) {
        LOGGER.debug("No more migrator jobs available.");
      } else {
        LOGGER.debug("Migrator jobs found: {}", migratorJobs.size());
      }

      migratorJobs.forEach(activatedJob -> {

        String legacyId = (String) activatedJob.getVariable("legacyId");

        ModifyProcessInstanceCommandStep1 modifyProcessInstance = camundaClient.newModifyProcessInstanceCommand(
                activatedJob.getProcessInstanceKey())
            // Cancel start event instance where migrator job sits to avoid executing the activities twice.
            .terminateElement(activatedJob.getElementInstanceKey()).and();

        ModifyProcessInstanceCommandStep3 modifyInstructions = null;
        ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(legacyId);
        Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree, new HashMap<>());

        for (String activityInstanceId : activityInstanceMap.keySet()) {
          ActInstance actInstance = activityInstanceMap.get(activityInstanceId);
          String activityId = actInstance.activityId();

          Map<String, Object> localVariables = new HashMap<>();

          VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery()
              .activityInstanceIdIn(activityInstanceId);

          paginate(variableQuery.count(), i -> new HashSet<>(variableQuery.list()),
              variable -> localVariables.put(variable.getName(), variable.getValue()));

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

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  record ActInstance(String activityId, String subProcessInstanceId) {
  }

  public void setRetryMode(boolean retryMode) {
    this.retryMode = retryMode;
  }

  protected <T> void paginate(Long maxCount, Function<Integer, Set<T>> query, Consumer<T> callback) {
    for (int offset = 0; offset < maxCount; offset = offset + batchSize) {
      LOGGER.debug("Max count: {}, offset: {}, batch size: {}", maxCount, offset, batchSize);
      query.apply(offset).forEach(callback);
    }
  }

}
