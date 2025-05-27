/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.HistoryMigrator.BATCH_SIZE;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.migrator.history.IdKeyDbModel;
import io.camunda.migrator.history.IdKeyMapper;

@Component
public class RuntimeMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeMigrator.class);
  public static final int DEFAULT_MAX_JOB_COUNT = 500;
  public static final int DEFAULT_MAX_PROCESS_INSTANCE = 100;

  protected int maxJobsToActivate = DEFAULT_MAX_JOB_COUNT;
  protected int maxProcessInstance = DEFAULT_MAX_PROCESS_INSTANCE;
  protected final Duration migratorJobsTimeout = Duration.ofMinutes(1);

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  private IdKeyMapper idKeyMapper;

  @Autowired
  protected CamundaClient camundaClient;


  public int getMaxJobsToActivate() {
    return maxJobsToActivate;
  }

  public void setMaxJobsToActivate(int maxJobsToActivate) {
    this.maxJobsToActivate = maxJobsToActivate;
  }

  public int getMaxProcessInstance() {
    return maxProcessInstance;
  }

  public void setMaxProcessInstance(int maxProcessInstance) {
    this.maxProcessInstance = maxProcessInstance;
  }

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

    int limit = maxProcessInstance;
    int offset = 0;  // Starting index
    List<String> processInstanceIds;

    do {
      // limit and offset are kept as we filter only non migrated instances
      processInstanceIds = idKeyMapper.findNonProcessInstanceIds(limit, offset);
      LOGGER.debug("Fetched instances to migrate: " + processInstanceIds.size());

      processInstanceIds.forEach(legacyProcessInstanceId -> {
        prepareDataAndCreateC8ProcessInstance(legacyProcessInstanceId);
      });
    } while (!processInstanceIds.isEmpty());

    activateMigratorJobsAndModifyInstances(); // TODO test multi level processes
    // doc idempotency
    // what if there are more than one migrator in a process instance
    LOGGER.debug("No more instances to migrate.");
  }

  private void prepareDataAndCreateC8ProcessInstance(String legacyProcessInstanceId) {
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
  }

  private void modifyProcessInstancePerJob(List<ActivatedJob> migratorJobs) {
    migratorJobs.forEach(activatedJob -> {

      String legacyId = (String) activatedJob.getVariable("legacyId");
      LOGGER.debug("Modify process instance for job: " + activatedJob);
      ModifyProcessInstanceCommandStep1 modifyProcessInstance = camundaClient.newModifyProcessInstanceCommand(
              activatedJob.getProcessInstanceKey())
          // Cancel start event instance where migrator job sits to avoid executing the activities twice.
          .terminateElement(activatedJob.getElementInstanceKey()).and();

      ModifyProcessInstanceCommandStep3 modifyInstructions = null;
      ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(legacyId);
      Map<String, ActInstance> activityInstanceMap = getActiveActivityIdsById(activityInstanceTree, new HashMap<>());

    for (String activityInstanceId : activityInstanceMap.keySet()) {
      ActInstance actInstance = activityInstanceMap.get(activityInstanceId);
      String activityId = actInstance.id().split("#multiInstanceBody")[0];

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
      LOGGER.debug("Modify instructions send.");
    });
  }

  private void activateMigratorJobsAndModifyInstances() {
    List<ActivatedJob> migratorJobs;
    boolean hasJobs = true;
    while (hasJobs) {
      migratorJobs = camundaClient.newActivateJobsCommand()
          .jobType("migrator")
          .maxJobsToActivate(maxJobsToActivate)
          .timeout(migratorJobsTimeout)
          .send()
          .join()
          .getJobs();
      if (migratorJobs.isEmpty()) {
        hasJobs = false;
        LOGGER.debug("No more migrator jobs available.");
      } else {
        LOGGER.debug("Migrator jobs found: " + migratorJobs.size());
        modifyProcessInstancePerJob(migratorJobs);
      }
    }
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
