/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.impl.util.ConverterUtil;
import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.util.List;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HistoryProcessInstanceTest extends HistoryMigrationAbstractTest {

  @Test
  public void shouldMigrateProcessInstances() {
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    // given
    ProcessInstance c7Process = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(c7Process.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);
    assertC8ProcessInstanceFields(processInstances.getFirst(), historicProcessInstance, "userTaskProcessId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, "custom-version-tag", null, false, false);
  }

  @Test
  public void shouldMigrateProcessInstancesWithTenant() {
    deployer.deployCamunda7Process("userTaskProcess.bpmn", "my-tenant1");

    // given
    ProcessInstance c7Process = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(c7Process.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances.size()).isEqualTo(1);
    assertC8ProcessInstanceFields(processInstances.getFirst(), historicProcessInstance, "userTaskProcessId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, "custom-version-tag", "my-tenant1", false, false);
  }

  @Test
  public void shouldMigrateCallActivityAndSubprocess() {
    // given
    deployer.deployCamunda7Process("callActivityProcess.bpmn");
    deployer.deployCamunda7Process("calledActivitySubprocess.bpmn");
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
    assertC8ProcessInstanceFields(parent, historicProcessInstance, "callingProcessId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, null, null, false, false);

    var sub = subProcessInstance.getFirst();
    assertC8ProcessInstanceFields(sub, historicSubProcessInstance, "calledProcessInstanceId",
        ProcessInstanceEntity.ProcessInstanceState.COMPLETED, null, null, true, false);

  }

  @Test
  public void shouldMigrateProcessInstanceWithIncident() {
    deployer.deployCamunda7Process("incidentProcess.bpmn");

    // given state in c7
    ProcessInstance c7Process = runtimeService.startProcessInstanceByKey("incidentProcessId");
    triggerIncident(c7Process.getId());
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(c7Process.getId())
        .singleResult();

    // when history is migrated
    historyMigrator.migrate();

    // then expected number of historic process instances
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("incidentProcessId");
    assertThat(processInstances.size()).isEqualTo(1);
    for (ProcessInstanceEntity processInstance : processInstances) {
      // and process instance has expected state
      assertC8ProcessInstanceFields(processInstance, historicProcessInstance, "incidentProcessId",
          ProcessInstanceEntity.ProcessInstanceState.ACTIVE, null, null, false, true);
    }
  }

  @Test
  @Disabled("https://github.com/camunda/camunda-bpm-platform/issues/5359")
  public void shouldCheckCalledProcessParentElementKey() {

    // given
    deployer.deployCamunda7Process("callActivityProcess.bpmn");
    deployer.deployCamunda7Process("calledActivitySubprocess.bpmn");
    ProcessInstance parentInstance = runtimeService.startProcessInstanceByKey("callingProcessId");
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(parentInstance.getProcessInstanceId())
        .singleResult();
    completeAllUserTasksWithDefaultUserTaskId();
    HistoricActivityInstance callActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("callActivityId")
        .processInstanceId(subInstance.getId())
        .singleResult();
    dbClient.insert(callActivity.getId(), null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);
    // when
    historyMigrator.migrate();

    // then
    assertThat(searchHistoricProcessInstances("calledProcessInstanceId")).isEmpty();
  }

  protected static void assertC8ProcessInstanceFields(ProcessInstanceEntity processInstance,
                                                      HistoricProcessInstance historicProcessInstance,
                                                      String processDefinitionId,
                                                      ProcessInstanceEntity.ProcessInstanceState processInstanceState,
                                                      String versionTag,
                                                      String tenantId,
                                                      boolean hasParent,
                                                      boolean hasIncidents) {
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

    assertThat(processInstance.hasIncident()).isEqualTo(hasIncidents);
    // https://github.com/camunda/camunda-bpm-platform/issues/5359
    assertThat(processInstance.treePath()).isNull();
    assertThat(processInstance.parentFlowNodeInstanceKey()).isNull();
  }

}
