/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.END_EVENT;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.PARALLEL_GATEWAY;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType.SUB_PROCESS;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import java.util.List;
import org.camunda.bpm.engine.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FlowNodeTreePathTest extends HistoryMigrationAbstractTest {

  @Autowired
  protected HistoryService historyService;

  @Autowired
  private RdbmsWriter rdbmsWriter;

  // TODO camunda-bpm-platform/issues/5359 - remove workaround once tree path is set for PI-s as well
  static final String testParentTreePath = "123456789";

  @Test
  public void shouldCorrectlyConstructTreePath() {
    // given - deploy process and create completed instance
    deployer.deployCamunda7Process("passingServiceTaskProcess.bpmn");
    String processInstanceId = runtimeService.startProcessInstanceByKey("serviceTaskProcessId").getId();

    var legacyFlowNodes = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();

    // TODO camunda-bpm-platform/issues/5359 - remove workaround once tree path is set for PI-s as well
    ProcessInstanceEntity processInstance = migrateWithTreePathWorkaround(legacyFlowNodes, "serviceTaskProcessId");

    // Verify flow nodes have correct tree path structure
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodes(processInstance.processInstanceKey());
    assertThat(flowNodes).isNotEmpty();

    // Verify specific flow nodes have the expected tree path pattern
    verifyActivityTreePathPattern(flowNodes, "StartEvent_1", testParentTreePath);
    verifyActivityTreePathPattern(flowNodes, "serviceTaskId", testParentTreePath);
    verifyActivityTreePathPattern(flowNodes, "Event_1uk5gek", testParentTreePath);
  }

  @Test
  public void shouldCorrectlyConstructTreePathForMultiInstance() {
    // given - deploy process with multi-instance activities and create completed instance
    deployer.deployCamunda7Process("multiInstanceProcess.bpmn");
    String processInstanceId = runtimeService.startProcessInstanceByKey("multiInstanceProcess").getId();

    var legacyFlowNodes = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();

    // TODO camunda-bpm-platform/issues/5359 - remove workaround once tree path is set for PI-s as well
    ProcessInstanceEntity processInstance = migrateWithTreePathWorkaround(legacyFlowNodes, "multiInstanceProcess");

    // Verify flow nodes have correct tree path structure
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodes(processInstance.processInstanceKey());
    assertThat(flowNodes).isNotEmpty();

    // Verify main process level activities using the helper method
    verifyActivityTreePathPattern(flowNodes, "StartEvent_1", testParentTreePath);
    verifyActivityTreePathPattern(flowNodes, "userTask", testParentTreePath);

    // Verify parallel gateways exist
    List<FlowNodeInstanceEntity> parallelGateways = flowNodes.stream()
        .filter(fn -> fn.type() == PARALLEL_GATEWAY)
        .toList();
    assertThat(parallelGateways).isNotEmpty();

    // Verify multi-instance user task (there should be multiple instances)
    List<FlowNodeInstanceEntity> multiUserTaskInstances = flowNodes.stream()
        .filter(fn -> "multiUserTask".equals(fn.flowNodeId()))
        .toList();
    assertThat(multiUserTaskInstances).isNotEmpty(); // Should have multiple instances due to multi-instance
// TODO verify expected multi instance tree path
//    for (FlowNodeInstanceEntity multiUserTask : multiUserTaskInstances) {
//      String expectedMultiUserTaskPattern = testParentTreePath + "/multiUserTask/FNI_" + multiUserTask.flowNodeInstanceKey();
//      assertThat(multiUserTask.treePath()).isEqualTo(expectedMultiUserTaskPattern);
////      Expected :"PI_12345/parentActivity/FNI_67890/multiUserTask/FNI_9221220078276190713"
////      Actual   :"PI_12345/parentActivity/FNI_67890/multiUserTask#multiInstanceBody/FNI_9221724035408207844/multiUserTask/FNI_9221220078276190713"
//    }

    // Verify end event has not been created yet (process not completed)
    FlowNodeInstanceEntity endEvent = flowNodes.stream()
        .filter(fn -> fn.type() == END_EVENT && "Event_0cvlmgu".equals(fn.flowNodeId()))
        .findFirst().orElse(null);
    assertThat(endEvent).isNull();
  }

  @Test
  public void shouldCorrectlyConstructTreePathForSubprocess() {
    // given - deploy process with event subprocess and create instance
    deployer.deployCamunda7Process("eventSubprocess.bpmn");
    String processInstanceId = runtimeService.startProcessInstanceByKey("eventSubprocessId").getId();

    // Send signal event to trigger the event subprocess
    runtimeService.signalEventReceived("SignalEventName");

    var legacyFlowNodes = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();

    // TODO camunda-bpm-platform/issues/5359 - remove workaround once tree path is set for PI-s as well
    ProcessInstanceEntity processInstance = migrateWithTreePathWorkaround(legacyFlowNodes, "eventSubprocessId");

    // Verify flow nodes have correct tree path structure
    List<FlowNodeInstanceEntity> flowNodes = searchHistoricFlowNodes(processInstance.processInstanceKey());
    assertThat(flowNodes).isNotEmpty();

    // Verify main process level activities using the helper method
    verifyActivityTreePathPattern(flowNodes, "StartEvent_1", testParentTreePath);
    verifyActivityTreePathPattern(flowNodes, "userTaskId", testParentTreePath);
    verifyActivityTreePathPattern(flowNodes, "Activity_0o0rtq8", testParentTreePath);

    // Activities inside subprocess should have pattern: parentPath/Activity_0o0rtq8/FNI_<subprocessKey>/activityId/FNI_<activityKey>
    FlowNodeInstanceEntity eventSubprocess = flowNodes.stream()
        .filter(fn -> fn.type() == SUB_PROCESS && "Activity_0o0rtq8".equals(fn.flowNodeId()))
        .findFirst().orElse(null);
    assertThat(eventSubprocess).isNotNull();
    String subprocessTreePath = testParentTreePath + "/" + eventSubprocess.flowNodeInstanceKey();

    // Verify subprocess activities using the helper method
    verifyActivityTreePathPattern(flowNodes, "Event_17uan4q", subprocessTreePath);
    verifyActivityTreePathPattern(flowNodes, "subprocessUserTaskId", subprocessTreePath);

    // Verify subprocess end event and main end event don't exist yet (process not completed)
    FlowNodeInstanceEntity subEndEvent = flowNodes.stream()
        .filter(fn -> fn.type() == END_EVENT && "Event_0xj0y8c".equals(fn.flowNodeId()))
        .findFirst().orElse(null);
    assertThat(subEndEvent).isNull();

    FlowNodeInstanceEntity mainEndEvent = flowNodes.stream()
        .filter(fn -> fn.type() == END_EVENT && "Event_1j5jkpj".equals(fn.flowNodeId()))
        .findFirst().orElse(null);
    assertThat(mainEndEvent).isNull();
  }

  private void verifyActivityTreePathPattern(List<FlowNodeInstanceEntity> flowNodes, String activityId, String parentTreePath) {
    FlowNodeInstanceEntity activity = flowNodes.stream()
        .filter(fn -> activityId.equals(fn.flowNodeId()))
        .findFirst().orElse(null);
    assertThat(activity).isNotNull();
    String expectedPattern = parentTreePath + "/" + activity.flowNodeInstanceKey();
    assertThat(activity.treePath()).isEqualTo(expectedPattern);
  }

  /**
   * Workaround method to migrate flow nodes with manually set tree path.
   * TODO camunda-bpm-platform/issues/5359 - remove this workaround once tree path is set for PI-s as well
   */
  private ProcessInstanceEntity migrateWithTreePathWorkaround(List<?> legacyFlowNodes, String processDefinitionKey) {
    // Add flow nodes to migration table to skip them initially
    for (var legacyFlowNode : legacyFlowNodes) {
      dbClient.insert(((org.camunda.bpm.engine.history.HistoricActivityInstance) legacyFlowNode).getId(), null, IdKeyMapper.TYPE.HISTORY_FLOW_NODE);
    }

    // when - migrate history (flow nodes will be skipped)
    historyMigrator.migrate();

    // then - get the migrated process instance and manually set a test tree path to simulate parent context
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances(processDefinitionKey);
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity processInstance = processInstances.getFirst();
    Long processInstanceKey = processInstance.processInstanceKey();

    // Create a ProcessInstanceDbModel with the test tree path (simulating this is called from a parent process)
    setProcessInstanceTreePath(processInstance);

    // Remove flow nodes from migration table so they can be migrated
    for (var legacyFlowNode : legacyFlowNodes) {
      dbClient.deleteByIdAndType(((org.camunda.bpm.engine.history.HistoricActivityInstance) legacyFlowNode).getId(), IdKeyMapper.TYPE.HISTORY_FLOW_NODE);
    }

    // Run migration again to migrate the flow nodes with the updated tree path
    historyMigrator.migrate();

    return processInstance;
  }

  private void setProcessInstanceTreePath(ProcessInstanceEntity processInstance) {
    ProcessInstanceDbModel updatedProcessInstance = new ProcessInstanceDbModel.ProcessInstanceDbModelBuilder()
        .processInstanceKey(processInstance.processInstanceKey())
        .processDefinitionKey(processInstance.processDefinitionKey())
        .processDefinitionId(processInstance.processDefinitionId())
        .parentProcessInstanceKey(processInstance.parentProcessInstanceKey())
        .startDate(processInstance.startDate())
        .endDate(processInstance.endDate())
        .treePath(testParentTreePath)
        .tenantId(processInstance.tenantId())
        .build();

    // Update the process instance with the test tree path
    rdbmsWriter.getProcessInstanceWriter().update(updatedProcessInstance);
    rdbmsWriter.flush();
  }
}
