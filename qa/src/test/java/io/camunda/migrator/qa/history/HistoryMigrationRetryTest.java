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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoryMigrationRetryTest extends HistoryMigrationAbstractTest {

  @Autowired
  private IdKeyMapper idKeyMapper;

  @Test
  public void shouldMigratePreviouslySkippedProcessDefinition() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    String c7Id = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(c7Id, HISTORY_PROCESS_DEFINITION);
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
    String c7Id = repositoryService.createDecisionDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(c7Id, IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION);

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
    String c7Id = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult().getId();
    markEntityAsSkipped(c7Id, IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENT);

    // when 
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then
    assertThat(searchHistoricDecisionRequirementsDefinition("simpleDmnWithReqsId").size()).isEqualTo(1);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENT)).isEqualTo(0);
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
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(procDefId, HISTORY_PROCESS_DEFINITION)).isTrue();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isTrue();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(actInstId, HISTORY_FLOW_NODE)).isTrue();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(taskId, HISTORY_USER_TASK)).isTrue();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(incidentId, HISTORY_INCIDENT)).isTrue();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(varId, HISTORY_VARIABLE)).isTrue();
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
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isFalse();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(actInstId, HISTORY_FLOW_NODE)).isFalse();
    assertThat(dbClient.checkHasC8KeyByC7IdAndType(taskId, HISTORY_USER_TASK)).isFalse();
  }

  @Test
  public void shouldUpdateSkipReasonOnRetry() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("userTaskProcessId");
    }
    completeAllUserTasksWithDefaultUserTaskId();

    // Mark process definition as skipped with initial reason
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    dbClient.insert(procDefId, null, null, HISTORY_PROCESS_DEFINITION, "Initial skip reason");

    // when: initial migration - process instances should be skipped due to missing process definition
    historyMigrator.migrate();

    // then: process instances should be skipped with missing process definition reason
    List<String> processInstanceIds = historyService.createHistoricProcessInstanceQuery()
        .list()
        .stream()
        .map(pi -> pi.getId())
        .toList();
    assertThat(processInstanceIds).hasSize(3);

    for (String procInstId : processInstanceIds) {
      assertThat(dbClient.checkExistsByC7IdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isTrue();
      assertThat(dbClient.checkHasC8KeyByC7IdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isFalse();
      // Verify initial skip reason from migration
      var skippedInstances = idKeyMapper.findSkippedByType(HISTORY_PROCESS_INSTANCE, 0, 100);
      var skippedInstance = skippedInstances.stream()
          .filter(si -> si.getC7Id().equals(procInstId))
          .findFirst()
          .orElse(null);
      assertThat(skippedInstance).isNotNull();
      assertThat(skippedInstance.getSkipReason()).isEqualTo("Missing process definition");
    }

    // when: retry migration with process definition still skipped but with different reason
    dbClient.updateSkipReason(procDefId, HISTORY_PROCESS_DEFINITION, "Updated skip reason");
    historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
    historyMigrator.migrate();

    // then: process instances should still be skipped with same skip reason
    // (because process definition is still missing)
    for (String procInstId : processInstanceIds) {
      assertThat(dbClient.checkHasC8KeyByC7IdAndType(procInstId, HISTORY_PROCESS_INSTANCE)).isFalse();
      var skippedInstances = idKeyMapper.findSkippedByType(HISTORY_PROCESS_INSTANCE, 0, 100);
      var skippedInstance = skippedInstances.stream()
          .filter(si -> si.getC7Id().equals(procInstId))
          .findFirst()
          .orElse(null);
      assertThat(skippedInstance).isNotNull();
      assertThat(skippedInstance.getSkipReason()).isEqualTo("Missing process definition");
    }
  }

  private void markEntityAsSkipped(String c7Id, IdKeyMapper.TYPE type) {
    dbClient.insert(c7Id, null, type);
  }
}
