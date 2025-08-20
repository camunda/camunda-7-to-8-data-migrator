/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history;

import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_FLOW_NODE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_INCIDENT;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_USER_TASK;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_VARIABLE;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.MigratorMode;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.util.List;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoryMigrationRetryTest extends HistoryMigrationAbstractTest {

  @Autowired
  private HistoryService historyService;

  @Autowired
  private ManagementService managementService;

  @Test
  public void shouldMigratePreviouslySkippedProcessDefinition() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    String legacyId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(legacyId, HISTORY_PROCESS_DEFINITION);

    // when history migration is retried
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then process definition is migrated and no longer skipped
    assertThat(searchHistoricProcessDefinitions("userTaskProcessId").size()).isEqualTo(1);
    assertThat(dbClient.countSkippedByType(HISTORY_PROCESS_DEFINITION)).isEqualTo(0);
  }

  @Test
  public void shouldMigratePreviouslySkippedDecisionDefinition() {
    // given
    deployer.deployCamunda7Decision("simpleDmn.dmn");
    String legacyId = repositoryService.createDecisionDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(legacyId, IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION);

    // when 
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then
    assertThat(searchHistoricDecisionDefinitions("simpleDecisionId").size()).isEqualTo(1);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION)).isEqualTo(0);
  }

  @Test
  public void shouldMigratePreviouslySkippedDecisionRequirementsDefinition() {
    // given
    deployer.deployCamunda7Decision("simpleDmnWithReqs.dmn");
    String legacyId = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(legacyId, IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENTS);

    // when 
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then
    assertThat(searchHistoricDecisionRequirementsDefinition("simpleDmnWithReqsId").size()).isEqualTo(1);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENTS)).isEqualTo(0);
  }

  @Test
  public void shouldMigrateOnlyPreviouslySkippedElementsOnRetry() {
    // given state in c7
    deployer.deployCamunda7Process("includeAllSupportedElementsProcess.bpmn");
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("allElementsProcessId");
    }
    completeAllUserTasksWithDefaultUserTaskId();
    executeAllJobsWithRetry();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    String procInstId = historyService.createHistoricProcessInstanceQuery().list().getFirst().getId();
    String actInstId = historyService.createHistoricActivityInstanceQuery()
        .activityType("userTask")
        .processInstanceId(procInstId)
        .list()
        .getFirst()
        .getId();
    String taskId = historyService.createHistoricTaskInstanceQuery()
        .activityInstanceIdIn(actInstId)
        .list()
        .getFirst()
        .getId();
    String incidentId = historyService.createHistoricIncidentQuery()
        .processInstanceId(procInstId)
        .list()
        .getFirst()
        .getId();
    String varId = historyService.createHistoricVariableInstanceQuery()
        .activityInstanceIdIn(actInstId)
        .singleResult()
        .getId();
    markEntityAsSkipped(procDefId, HISTORY_PROCESS_DEFINITION);
    markEntityAsSkipped(procInstId, HISTORY_PROCESS_INSTANCE);
    markEntityAsSkipped(actInstId, HISTORY_FLOW_NODE);
    markEntityAsSkipped(taskId, HISTORY_USER_TASK);
    markEntityAsSkipped(varId, HISTORY_VARIABLE);
    markEntityAsSkipped(incidentId, HISTORY_INCIDENT);

    // when migration is retried
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then only previously skipped entities are migrated
    assertThat(searchHistoricProcessDefinitions("allElementsProcessId").size()).isEqualTo(1);
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("allElementsProcessId");
    assertThat(processInstances.size()).isEqualTo(1);
    assertThat(searchHistoricUserTasks(processInstances.getFirst().processInstanceKey()).size()).isEqualTo(1);
    assertThat(searchHistoricIncidents("allElementsProcessId").size()).isEqualTo(1);
    assertThat(searchHistoricVariables("userTaskVar").size()).isEqualTo(1);

    // and nothing marked as skipped
    assertThat(dbClient.checkHasKeyByIdAndType(procDefId, HISTORY_PROCESS_DEFINITION)).isTrue();
    assertThat(dbClient.checkHasKeyByIdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isTrue();
    assertThat(dbClient.checkHasKeyByIdAndType(actInstId, HISTORY_FLOW_NODE)).isTrue();
    assertThat(dbClient.checkHasKeyByIdAndType(taskId, HISTORY_USER_TASK)).isTrue();
    assertThat(dbClient.checkHasKeyByIdAndType(incidentId, HISTORY_INCIDENT)).isTrue();
    assertThat(dbClient.checkHasKeyByIdAndType(varId, HISTORY_VARIABLE)).isTrue();
  }

  @Test
  public void shouldNotMigratePreviouslySkippedElementsOnRerun() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("userTaskProcessId");
    }
    completeAllUserTasksWithDefaultUserTaskId();

    // and some entities manually set as skipped
    String procInstId = historyService.createHistoricProcessInstanceQuery().list().getFirst().getId();
    String actInstId = historyService.createHistoricActivityInstanceQuery()
        .activityType("userTask")
        .processInstanceId(procInstId)
        .list()
        .getFirst()
        .getId();
    String taskId = historyService.createHistoricTaskInstanceQuery()
        .activityInstanceIdIn(actInstId)
        .list()
        .getFirst()
        .getId();

    markEntityAsSkipped(procInstId, HISTORY_PROCESS_INSTANCE);
    markEntityAsSkipped(actInstId, HISTORY_FLOW_NODE);
    markEntityAsSkipped(taskId, HISTORY_USER_TASK);

    // when migration is run on migrate mode
    historyMigrator.migrate();

    // then only non skipped entities are migrated
    assertThat(searchHistoricProcessDefinitions("userTaskProcessId").size()).isEqualTo(1);
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances.size()).isEqualTo(4);

    // and skipped entities are still skipped
    assertThat(dbClient.checkHasKeyByIdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isFalse();
    assertThat(dbClient.checkHasKeyByIdAndType(actInstId, HISTORY_FLOW_NODE)).isFalse();
    assertThat(dbClient.checkHasKeyByIdAndType(taskId, HISTORY_USER_TASK)).isFalse();
  }

  private void executeAllJobsWithRetry() {
    var jobs = managementService.createJobQuery().list();

    // Try executing the job multiple times to ensure incident is created
    for (var job : jobs) {
      for (int i = 0; i < 3; i++) {
        try {
          managementService.executeJob(job.getId());
        } catch (Exception e) {
          // expected - job will fail due to empty delegate expression
        }
      }
    }
  }

  private void markEntityAsSkipped(String legacyId, IdKeyMapper.TYPE type) {
    dbClient.insert(legacyId, null, type);
  }
}
