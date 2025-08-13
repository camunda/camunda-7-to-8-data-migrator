/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.impl.util.ConverterUtil;
import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.util.List;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

public class HistoryProcessInstanceTest extends HistoryMigrationAbstractTest {

  @Test
  public void shouldMigrateProcessInstances() {
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    // given state in c7
    ProcessInstance c7PI = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(c7PI.getId())
        .singleResult();

    // when history is migrated
    historyMigrator.migrate();

    // then expected number of historic process instances
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances.size()).isEqualTo(1);
    for (ProcessInstanceEntity processInstance : processInstances) {
      // and process instance has expected state
      assertThat(processInstance.state()).isEqualTo(ProcessInstanceEntity.ProcessInstanceState.COMPLETED);
      assertPIFields(processInstance, historicProcessInstance, "userTaskProcessId",
          ProcessInstanceEntity.ProcessInstanceState.COMPLETED, "custom-version-tag", null, false);
    }
  }

  @Test
  public void shouldMigrateProcessInstancesWithTenant() {
    deployer.deployCamunda7Process("userTaskProcess.bpmn", "my-tenant1");

    // given state in c7
    ProcessInstance c7PI = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(c7PI.getId())
        .singleResult();

    // when history is migrated
    historyMigrator.migrate();

    // then expected number of historic process instances
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances.size()).isEqualTo(1);
    assertPIFields(processInstances.getFirst(), historicProcessInstance, "userTaskProcessId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, "custom-version-tag", "my-tenant1", false);
  }

  @Test
  public void shouldMigrateCallActivityAndSubprocess() {
    // given
    deployer.deployCamunda7Process("calledActivitySubprocess.bpmn");
    deployer.deployCamunda7Process("callActivityProcess.bpmn");
    ProcessInstance parentInstance = runtimeService.startProcessInstanceByKey("callingProcessId");
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(parentInstance.getProcessInstanceId())
        .singleResult();
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(parentInstance.getId())
        .singleResult();
    HistoricProcessInstance historicSubProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(subInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> parentProcessInstance = searchHistoricProcessInstances("callingProcessId");
    List<ProcessInstanceEntity> subProcessInstance = searchHistoricProcessInstances("calledProcessInstanceId");
    assertThat(parentProcessInstance.size()).isEqualTo(1);
    assertThat(subProcessInstance.size()).isEqualTo(1);

    var parent = parentProcessInstance.getFirst();
    assertPIFields(parent, historicProcessInstance, "callingProcessId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, null, null, false);

    var sub = subProcessInstance.getFirst();
    assertPIFields(sub, historicSubProcessInstance, "calledProcessInstanceId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, null, null, true);

  }

  protected static void assertPIFields(ProcessInstanceEntity processInstance,
                                       HistoricProcessInstance historicProcessInstance,
                                       String processDefinitionId,
                                       ProcessInstanceEntity.ProcessInstanceState processInstanceState,
                                       String versionTag,
                                       String tenantId,
                                       boolean hasParent) {
    assertThat(processInstance.processDefinitionId()).isEqualTo(processDefinitionId);
    assertThat(processInstance.state()).isEqualTo(processInstanceState);
    assertThat(processInstance.processInstanceKey()).isNotNull();
    assertThat(processInstance.processDefinitionKey()).isNotNull();
    assertThat(processInstance.tenantId()).isEqualTo(tenantId);
    assertThat(processInstance.startDate()).isEqualTo(
        ConverterUtil.convertDate(historicProcessInstance.getStartTime()));
    assertThat(processInstance.endDate()).isEqualTo(ConverterUtil.convertDate(historicProcessInstance.getEndTime()));
    assertThat(processInstance.processDefinitionVersion()).isEqualTo(1);
    assertThat(processInstance.processDefinitionVersionTag()).isEqualTo(versionTag);

    if (hasParent) {
      assertThat(processInstance.parentProcessInstanceKey()).isNotNull();
      assertThat(processInstance.parentFlowNodeInstanceKey()).isNull();
    } else {
      assertThat(processInstance.parentProcessInstanceKey()).isNull();
      assertThat(processInstance.parentFlowNodeInstanceKey()).isNull();
    }

    assertThat(processInstance.hasIncident()).isFalse();
    //    assertThat(processInstance.treePath()).isNotNull();
  }

}
