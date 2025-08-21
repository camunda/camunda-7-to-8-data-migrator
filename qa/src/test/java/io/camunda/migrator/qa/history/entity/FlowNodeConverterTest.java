/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.ACTIVE;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.COMPLETED;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.TERMINATED;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.END_EVENT;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.PARALLEL_GATEWAY;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.SERVICE_TASK;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.START_EVENT;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.USER_TASK;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;

public class FlowNodeConverterTest extends HistoryMigrationAbstractTest {

  @Test
  public void shouldCorrectlyConvertFlowNodeFields() {
    // given - deploy process and create completed instance
    deployer.deployCamunda7Process("flowNodeHistoricMigrationTestProcess.bpmn");

    String processInstanceId = runtimeService.startProcessInstanceByKey("flowNodeHistoricMigrationTestProcessId",
        Map.of("testVar", "testValue")).getId();

    // Complete user task to generate completed flow nodes
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    // when - migrate history
    historyMigrator.migrate();

    // then - verify flow nodes are correctly migrated with all fields set
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("flowNodeHistoricMigrationTestProcessId");
    assertThat(processInstances).hasSize(1);

    Long processInstanceKey = processInstances.get(0).processInstanceKey();
    String processDefinitionId = processInstances.get(0).processDefinitionId();

    // Verify all flow node types and their fields are correctly converted
    verifyFlowNodeFields(processInstanceKey, START_EVENT, "StartEvent_1", processDefinitionId, COMPLETED);
    verifyFlowNodeFields(processInstanceKey, PARALLEL_GATEWAY, "ParallelGateway_Split", processDefinitionId, COMPLETED);
    verifyFlowNodeFields(processInstanceKey, USER_TASK, "userTaskId", processDefinitionId, COMPLETED);
    verifyFlowNodeFields(processInstanceKey, SERVICE_TASK, "serviceTaskId", processDefinitionId, COMPLETED);
    verifyFlowNodeFields(processInstanceKey, PARALLEL_GATEWAY, "ParallelGateway_Join", processDefinitionId, COMPLETED);
    verifyFlowNodeFields(processInstanceKey, END_EVENT, "EndEvent_1", processDefinitionId, COMPLETED);
  }

  @Test
  public void shouldCorrectlyConvertFlowNodeStates() {
    // Deploy both processes for different state scenarios
    deployer.deployCamunda7Process("flowNodeHistoricMigrationTestProcess.bpmn");
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    // Scenario 1: Active process (comprehensive process with incomplete user task)
    String activeProcessId = runtimeService.startProcessInstanceByKey("flowNodeHistoricMigrationTestProcessId").getId();

    // Scenario 2: Terminated process (simple user task process)
    String terminatedProcessId = runtimeService.startProcessInstanceByKey("userTaskProcessId").getId();
    runtimeService.deleteProcessInstance(terminatedProcessId, "Test termination");

    // when - migrate all history
    historyMigrator.migrate();

    // then - verify ACTIVE state
    List<ProcessInstanceEntity> activeProcessInstances = searchHistoricProcessInstances("flowNodeHistoricMigrationTestProcessId");
    assertThat(activeProcessInstances).hasSize(1);
    Long activeProcessKey = activeProcessInstances.get(0).processInstanceKey();

    // User task should be ACTIVE (endTime is null)
    List<FlowNodeInstanceEntity> activeUserTasks = searchHistoricFlowNodesForType(activeProcessKey, USER_TASK);
    assertThat(activeUserTasks).hasSize(1);
    assertThat(activeUserTasks.getFirst().state()).isEqualTo(ACTIVE);
    assertThat(activeUserTasks.getFirst().endDate()).isNull();

    // Other flow nodes should be COMPLETED
    List<FlowNodeInstanceEntity> startEvents = searchHistoricFlowNodesForType(activeProcessKey, START_EVENT);
    assertThat(startEvents.getFirst().state()).isEqualTo(COMPLETED);
    assertThat(startEvents.getFirst().endDate()).isNotNull();

    // Verify TERMINATED state
    ProcessInstanceEntity terminatedInstance = searchHistoricProcessInstances("userTaskProcessId").getFirst();
    assertThat(terminatedInstance.state()).isEqualTo(ProcessInstanceEntity.ProcessInstanceState.CANCELED);
    List<FlowNodeInstanceEntity> terminatedUserTask = searchHistoricFlowNodesForType(terminatedInstance.processInstanceKey(), USER_TASK);
    assertThat(terminatedUserTask).hasSize(1);
    assertThat(terminatedUserTask.getFirst().state()).isEqualTo(TERMINATED);
  }

  private void verifyFlowNodeFields(Long processInstanceKey,
                                   FlowNodeInstanceEntity.FlowNodeType expectedType,
                                   String expectedFlowNodeId,
                                   String expectedProcessDefinitionId,
                                   FlowNodeInstanceEntity.FlowNodeState expectedState) {
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodesForType(processInstanceKey, expectedType)
        .stream()
        .filter(fn -> expectedFlowNodeId.equals(fn.flowNodeId()))
        .toList();

    // For parallel gateways, there might be multiple instances due to parallel execution
    if (expectedType == PARALLEL_GATEWAY && expectedFlowNodeId.equals("ParallelGateway_Join")) {
      assertThat(flowNodes).hasSizeGreaterThanOrEqualTo(1);
    } else {
      assertThat(flowNodes).hasSize(1);
    }

    // Verify all instances have correct fields (use the first one for detailed verification)
    FlowNodeInstanceEntity flowNode = flowNodes.getFirst();

    // Verify all converted fields
    assertThat(flowNode.flowNodeInstanceKey()).isNotNull().isPositive();
    assertThat(flowNode.flowNodeId()).isEqualTo(expectedFlowNodeId);
    assertThat(flowNode.processInstanceKey()).isEqualTo(processInstanceKey);
    assertThat(flowNode.processDefinitionKey()).isNotNull().isPositive();
    assertThat(flowNode.processDefinitionId()).isEqualTo(expectedProcessDefinitionId);
    assertThat(flowNode.type()).isEqualTo(expectedType);
    assertThat(flowNode.state()).isEqualTo(expectedState);

    // Verify date fields
    assertThat(flowNode.startDate()).isNotNull();
    if (expectedState == COMPLETED || expectedState == TERMINATED) {
      assertThat(flowNode.endDate()).isNotNull();
      assertThat(flowNode.endDate()).isAfterOrEqualTo(flowNode.startDate());
    }

    // Verify tenant ID is handled correctly (should be null for this test)
    assertThat(flowNode.tenantId()).isNull();

    // Verify fields that are not yet supported in C7 migration
    assertThat(flowNode.treePath()).isNull(); // Not supported in C7
    assertThat(flowNode.incidentKey()).isNull(); // Not supported in C7

    // If there are multiple instances (like for join gateway), verify they all have consistent fields
    if (flowNodes.size() > 1) {
      for (FlowNodeInstanceEntity additionalNode : flowNodes) {
        assertThat(additionalNode.flowNodeId()).isEqualTo(expectedFlowNodeId);
        assertThat(additionalNode.processInstanceKey()).isEqualTo(processInstanceKey);
        assertThat(additionalNode.type()).isEqualTo(expectedType);
        assertThat(additionalNode.state()).isEqualTo(expectedState);
      }
    }
  }

  private List<FlowNodeInstanceEntity> searchHistoricFlowNodes(Long processInstanceKey) {
    return rdbmsService.getFlowNodeInstanceReader()
        .search(io.camunda.search.query.FlowNodeInstanceQuery.of(queryBuilder ->
            queryBuilder.filter(filterBuilder ->
                filterBuilder.processInstanceKeys(processInstanceKey))))
        .items();
  }
}
