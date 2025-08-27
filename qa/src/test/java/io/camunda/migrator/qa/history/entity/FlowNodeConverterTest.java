/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static io.camunda.client.ClientProperties.DEFAULT_TENANT_ID;
import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.ACTIVE;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.COMPLETED;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState.TERMINATED;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.END_EVENT;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.PARALLEL_GATEWAY;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.SERVICE_TASK;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.START_EVENT;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.USER_TASK;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FlowNodeConverterTest extends HistoryMigrationAbstractTest {

  @Autowired
  private ManagementService managementService;

  @Autowired
  protected HistoryService historyService;

  @Autowired
  private RdbmsWriter rdbmsWriter;

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
    List<HistoricActivityInstance> legacyFlowNodes = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();


    // when - migrate history
    historyMigrator.migrate();

    // then - verify flow nodes are correctly migrated with all fields set
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances(
        "flowNodeHistoricMigrationTestProcessId");
    assertThat(processInstances).hasSize(1);

    Long processInstanceKey = processInstances.getFirst().processInstanceKey();
    String processDefinitionId = processInstances.getFirst().processDefinitionId();
    Long processDefinitionKey = processInstances.getFirst().processDefinitionKey();

    // Verify all flow node types and their fields are correctly converted
    verifyFlowNodeFields(processInstanceKey, START_EVENT, "StartEvent_1", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
    verifyFlowNodeFields(processInstanceKey, PARALLEL_GATEWAY, "ParallelGateway_Split", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
    verifyFlowNodeFields(processInstanceKey, USER_TASK, "userTaskId", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
    verifyFlowNodeFields(processInstanceKey, SERVICE_TASK, "serviceTaskId", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
    verifyFlowNodeFields(processInstanceKey, PARALLEL_GATEWAY, "ParallelGateway_Join", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
    verifyFlowNodeFields(processInstanceKey, END_EVENT, "EndEvent_1", processDefinitionId, COMPLETED, legacyFlowNodes, processDefinitionKey);
  }

  @Test
  public void shouldCorrectlyConvertFlowNodeStates() {
    // Deploy both processes for different state scenarios
    deployer.deployCamunda7Process("flowNodeHistoricMigrationTestProcess.bpmn");
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    // Scenario 1: Active process (comprehensive process with incomplete user task)
    String completedProcessInstanceId = runtimeService.startProcessInstanceByKey("flowNodeHistoricMigrationTestProcessId").getId();

    // Scenario 2: Terminated process (simple user task process)
    String terminatedProcessId = runtimeService.startProcessInstanceByKey("userTaskProcessId").getId();
    runtimeService.deleteProcessInstance(terminatedProcessId, "Test termination");
    List<HistoricActivityInstance> historicActivityInstancesForCanceled =  historyService.createHistoricActivityInstanceQuery().processInstanceId(terminatedProcessId).list();
    List<HistoricActivityInstance> historicActivityInstancesForCompleted = historyService.createHistoricActivityInstanceQuery().processInstanceId(completedProcessInstanceId).list();

    // when - migrate all history
    historyMigrator.migrate();

    // then - verify ACTIVE state
    List<ProcessInstanceEntity> activeProcessInstances = searchHistoricProcessInstances(
        "flowNodeHistoricMigrationTestProcessId");
    assertThat(activeProcessInstances).hasSize(1);
    Long activeProcessKey = activeProcessInstances.getFirst().processInstanceKey();

    // User task should be ACTIVE (endTime is null)
    List<FlowNodeInstanceEntity> activeUserTasks = searchHistoricFlowNodesForType(activeProcessKey, USER_TASK);
    assertThat(activeUserTasks).hasSize(1);
    assertThat(activeUserTasks.getFirst().state()).isEqualTo(ACTIVE);
    assertThat(activeUserTasks.getFirst().endDate()).isNull();

    // Other flow nodes should be COMPLETED
    List<FlowNodeInstanceEntity> startEvents = searchHistoricFlowNodesForType(activeProcessKey, START_EVENT);
    assertThat(startEvents.getFirst().state()).isEqualTo(COMPLETED);
    assertThat(startEvents.getFirst().endDate()).isEqualTo(extractEndDate(historicActivityInstancesForCompleted, "StartEvent_1"));

    // Verify TERMINATED state
    ProcessInstanceEntity terminatedInstance = searchHistoricProcessInstances("userTaskProcessId").getFirst();
    assertThat(terminatedInstance.state()).isEqualTo(ProcessInstanceEntity.ProcessInstanceState.CANCELED);
    List<FlowNodeInstanceEntity> terminatedUserTask = searchHistoricFlowNodesForType(
        terminatedInstance.processInstanceKey(), USER_TASK);
    assertThat(terminatedUserTask).hasSize(1);
    assertThat(terminatedUserTask.getFirst().state()).isEqualTo(TERMINATED);
  }

  @Test
  public void shouldCorrectlyHandleFlowNodeWithIncident() {
    // Deploy the BPMN with script task that will fail
    deployer.deployCamunda7Process("failOnceServiceTask.bpmn");
    final ProcessInstance processInstance = startProcessInstanceAndFailWithIncident("failOneServiceTaskProcessId");
    String processInstanceId = processInstance.getId();
    retryAndSucceed(processInstance);

    // Check for incidents
    var historicIncidents = historyService.createHistoricIncidentQuery().processInstanceId(processInstanceId).list();
    assertThat(historicIncidents).hasSize(1);

    // when - migrate history
    historyMigrator.migrate();
    // writer needs to be flushed for incidents to bewritten into the flow node table
    rdbmsWriter.flush();

    // then - verify process instances are migrated
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances(
          "failOneServiceTaskProcessId");
    assertThat(processInstances).hasSize(1);
    Long processInstanceKey = processInstances.getFirst().processInstanceKey();
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodesForType(processInstanceKey, SERVICE_TASK).stream().toList();
    assertThat(flowNodes).hasSize(1);
    FlowNodeInstanceEntity serviceTask = flowNodes.getFirst();
    assertThat(serviceTask.flowNodeId()).isEqualTo("serviceTaskActivityId");
    assertThat(serviceTask.hasIncident()).isTrue();
    assertThat(flowNodes).isNotEmpty();
  }

  private void verifyFlowNodeFields(Long processInstanceKey,
                                    FlowNodeInstanceEntity.FlowNodeType expectedType,
                                    String expectedFlowNodeId,
                                    String expectedProcessDefinitionId,
                                    FlowNodeInstanceEntity.FlowNodeState expectedState,
                                    List<HistoricActivityInstance> legacyFlowNodes,
                                    Long expectedProcessDefinitionKey) {
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodesForType(processInstanceKey, expectedType).stream()
        .filter(fn -> expectedFlowNodeId.equals(fn.flowNodeId()))
        .toList();

    // Get all legacy flow nodes with the same activity ID
    List<HistoricActivityInstance> matchingLegacyNodes = legacyFlowNodes.stream()
        .filter(h -> expectedFlowNodeId.equals(h.getActivityId()))
        .toList();

    // For parallel gateways, there might be multiple instances due to parallel execution
    if (expectedType == PARALLEL_GATEWAY && expectedFlowNodeId.equals("ParallelGateway_Join")) {
      assertThat(flowNodes).hasSizeGreaterThanOrEqualTo(1);
      assertThat(flowNodes).hasSameSizeAs(matchingLegacyNodes);
    } else {
      assertThat(flowNodes).hasSize(1);
    }

    for (FlowNodeInstanceEntity flowNode : flowNodes) {
      // Verify all converted fields
      assertThat(flowNode.flowNodeInstanceKey()).isNotNull().isPositive();
      assertThat(flowNode.flowNodeId()).isEqualTo(expectedFlowNodeId);
      assertThat(flowNode.processInstanceKey()).isEqualTo(processInstanceKey);
      assertThat(flowNode.processDefinitionKey()).isEqualTo(expectedProcessDefinitionKey);
      assertThat(flowNode.processDefinitionId()).isEqualTo(expectedProcessDefinitionId);
      assertThat(flowNode.type()).isEqualTo(expectedType);
      assertThat(flowNode.state()).isEqualTo(expectedState);
      assertThat(flowNode.treePath()).endsWith(flowNode.flowNodeInstanceKey().toString());
      assertThat(flowNode.incidentKey()).isNull();
      assertThat(flowNode.tenantId()).isEqualTo(DEFAULT_TENANT_ID);

      // Verify date fields
      if (expectedState == COMPLETED || expectedState == TERMINATED) {
        // For activities with multiple instances, verify that the end date exists in the legacy nodes
        if (matchingLegacyNodes.size() > 1) {
          List<OffsetDateTime> legacyStartDates = extractStartDates(legacyFlowNodes, expectedFlowNodeId);
          List<OffsetDateTime> legacyEndDates = extractEndDates(legacyFlowNodes, expectedFlowNodeId);
          assertThat(legacyStartDates).contains(flowNode.startDate());
          assertThat(legacyEndDates).contains(flowNode.endDate());
        } else {
          assertThat(flowNode.startDate()).isEqualTo(extractStartDate(legacyFlowNodes, expectedFlowNodeId));
          assertThat(flowNode.endDate()).isEqualTo(extractEndDate(legacyFlowNodes, expectedFlowNodeId));
        }
      }
    }
  }

  private void retryAndSucceed(final ProcessInstance processInstance) {
    runtimeService.setVariable(processInstance.getId(), "fail", false);
    String jobId = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId();
    managementService.setJobRetries(jobId, 1);
    managementService.executeJob(jobId);
  }

  private ProcessInstance startProcessInstanceAndFailWithIncident(String processDefinitionKey) {
    ProcessInstance processInstance = null;

    try {
      processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);
      String jobId = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId();
      managementService.setJobRetries(jobId, 0);
    } catch (ProcessEngineException ignored) {
    }
    return processInstance;
  }

  private static OffsetDateTime extractEndDate(List<HistoricActivityInstance> historicInstances, String activityId) {
    List<OffsetDateTime> endDates = extractEndDates(historicInstances, activityId);
    assertThat(endDates).hasSize(1);
    return endDates.getFirst();
  }

  private static List<OffsetDateTime> extractEndDates(List<HistoricActivityInstance> historicInstances, String activityId) {
    return extractDates(historicInstances, activityId, HistoricActivityInstance::getEndTime);
  }

  private static OffsetDateTime extractStartDate(List<HistoricActivityInstance> historicInstances, String activityId) {
    List<OffsetDateTime> startDates = extractStartDates(historicInstances, activityId);
    assertThat(startDates).hasSize(1);
    return startDates.getFirst();
  }

  private static List<OffsetDateTime> extractStartDates(List<HistoricActivityInstance> historicInstances, String activityId) {
    return extractDates(historicInstances, activityId, HistoricActivityInstance::getStartTime);
  }

  private static List<OffsetDateTime> extractDates(List<HistoricActivityInstance> historicInstances,
                                                   String activityId,
                                                   java.util.function.Function<HistoricActivityInstance, java.util.Date> dateAccessor) {
    return historicInstances.stream()
        .filter(h -> h.getActivityId().equals(activityId))
        .map(h -> convertDate(dateAccessor.apply(h)))
        .toList();
  }

}
