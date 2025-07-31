/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class HistoryMigrationSkippingTest extends HistoryMigrationAbstractTest {

  @Autowired
  protected DbClient dbClient;

  @Autowired
  private ManagementService managementService;

  @Test
  public void shouldSkipElementsWhenProcessDefinitionIsSkipped() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    for(int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("userTaskProcessId");
    }
    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }

    // and the process definitions is manually set as skipped
    String legacyId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    dbClient.insert(legacyId, null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);

    // when history is migrated
    historyMigrator.migrate();

    // then nothing was migrated
    assertThat(searchHistoricProcessInstances("userTaskProcessId").size()).isEqualTo(0);

    // and all elements for the definition were skipped skipped
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE)).isEqualTo(5);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_FLOW_NODE)).isEqualTo(15);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_USER_TASK)).isEqualTo(5);
  }

  @Test
  public void shouldSkipUserTasksWhenProcessInstanceIsSkipped() {
    // given state in c7
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    var processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    var task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // and the process instance is manually set as skipped
    dbClient.insert(processInstance.getId(), null, IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE);

    // when history is migrated
    historyMigrator.migrate();

    // then process instance and user task were skipped
    assertThat(searchHistoricProcessInstances("userTaskProcessId").size()).isEqualTo(0);
    assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_USER_TASK)).isEqualTo(1);
  }

  @Test
  public void shouldSkipIncidentsWhenProcessInstanceIsSkipped() {
        // given state in c7
        deployer.deployCamunda7Process("failingServiceTaskProcess.bpmn");
        var processInstance = runtimeService.startProcessInstanceByKey("failingServiceTaskProcessId");

        // execute the job to trigger the incident
        var jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(1);

        // Try executing the job multiple times to ensure incident is created
        for (int i = 0; i < 3; i++) {
          try {
            managementService.executeJob(jobs.get(0).getId());
          } catch (Exception e) {
            // expected - job will fail due to empty delegate expression
          }
        }

        // Wait briefly and verify incident is created
        try {
          Thread.sleep(500); // Give it a moment to create the incident
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        var incidents = historyService.createHistoricIncidentQuery().list();
        assertThat(historyService.createHistoricIncidentQuery().count()).as("Expected one incident to be created").isEqualTo(1);

        // and the process instance is manually set as skipped
        dbClient.insert(processInstance.getId(), null, IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE);

        // when history is migrated
        historyMigrator.migrate();

        // then process instance and incidents were skipped
        assertThat(searchHistoricProcessInstances("failingServiceTaskProcessId").size()).isEqualTo(0);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_INCIDENT)).isEqualTo(1);
    }


}
