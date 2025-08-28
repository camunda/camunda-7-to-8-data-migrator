/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import static io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder;
import static io.camunda.migrator.constants.MigratorConstants.C7_HISTORY_PARTITION_ID;
import static io.camunda.migrator.impl.util.C7Utils.MULTI_INSTANCE_BODY_SUFFIX;
import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static io.camunda.migrator.impl.util.ConverterUtil.getTenantId;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.service.FlowNodeInstanceWriter;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.history.HistoricActivityInstance;

public class FlowNodeConverter {

  public FlowNodeInstanceDbModel apply(HistoricActivityInstance legacyFlowNode,
                                       Long processDefinitionKey,
                                       Long processInstanceKey,
                                       Long flowNodeScopeKey,
                                       String parentTreePath) {
    Long flowNodeInstanceKey = getNextKey();

    return new FlowNodeInstanceDbModelBuilder()
        .historyCleanupDate(convertDate(legacyFlowNode.getRemovalTime()))
        .partitionId(C7_HISTORY_PARTITION_ID)
        .flowNodeName(legacyFlowNode.getActivityName())
        .flowNodeScopeKey(flowNodeScopeKey)
        .flowNodeInstanceKey(flowNodeInstanceKey)
        .flowNodeId(getFlowNodeId(legacyFlowNode))
        .processInstanceKey(processInstanceKey)
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(legacyFlowNode.getProcessDefinitionKey())
        .startDate(convertDate(legacyFlowNode.getStartTime()))
        .endDate(convertDate(legacyFlowNode.getEndTime()))
        .type(convertType(legacyFlowNode.getActivityType()))
        .tenantId(getTenantId(legacyFlowNode.getTenantId()))
        .state(getState(legacyFlowNode))
        .treePath(buildTreePath(parentTreePath, flowNodeInstanceKey))
        .incidentKey(null) // will be set when incident is created
        .hasIncident(false) // will be set when incident is created
        .numSubprocessIncidents(null)
        .build();
  }

  private String getFlowNodeId(HistoricActivityInstance legacyFlowNode) {
    return legacyFlowNode.getActivityId().replace(MULTI_INSTANCE_BODY_SUFFIX, "");
  }

  private String buildTreePath(String parentTreePath, Long flowNodeInstanceKey) {
    return String.join("/", parentTreePath, (flowNodeInstanceKey.toString()));
  }

  public void registerIncident(FlowNodeInstanceWriter flowNodeInstanceWriter, FlowNodeInstanceDbModel flowNodeInstance, Long incidentKey) {
    flowNodeInstanceWriter.createIncident(flowNodeInstance.flowNodeInstanceKey(), incidentKey);
  }

  public void registerSubprocessIncident(FlowNodeInstanceWriter flowNodeInstanceWriter, FlowNodeInstanceDbModel flowNodeInstance) {
    flowNodeInstanceWriter.createSubprocessIncident(flowNodeInstance.flowNodeInstanceKey());
  }


  protected FlowNodeInstanceEntity.FlowNodeState getState(HistoricActivityInstance legacyFlowNode) {
    if (legacyFlowNode.getEndTime() == null) {
      return FlowNodeInstanceEntity.FlowNodeState.ACTIVE;
    } else if (legacyFlowNode.isCanceled()) {
      return FlowNodeInstanceEntity.FlowNodeState.TERMINATED;
    } else {
      return FlowNodeInstanceEntity.FlowNodeState.COMPLETED;
    }
  }

//  In c7 for example we have event subscriptions
  protected FlowNodeType convertType(String activityType) {
    return switch (activityType) {
      // Start Events
      case ActivityTypes.START_EVENT, ActivityTypes.START_EVENT_TIMER, ActivityTypes.START_EVENT_MESSAGE,
           ActivityTypes.START_EVENT_ERROR, ActivityTypes.START_EVENT_SIGNAL, ActivityTypes.START_EVENT_ESCALATION,
           ActivityTypes.START_EVENT_COMPENSATION, ActivityTypes.START_EVENT_CONDITIONAL -> FlowNodeType.START_EVENT;

      // End Events
      case ActivityTypes.END_EVENT_NONE, ActivityTypes.END_EVENT_CANCEL, ActivityTypes.END_EVENT_ERROR,
           ActivityTypes.END_EVENT_TERMINATE, ActivityTypes.END_EVENT_MESSAGE, ActivityTypes.END_EVENT_SIGNAL,
           ActivityTypes.END_EVENT_COMPENSATION, ActivityTypes.END_EVENT_ESCALATION -> FlowNodeType.END_EVENT;

      // Tasks
      case ActivityTypes.TASK_SERVICE -> FlowNodeType.SERVICE_TASK;
      case ActivityTypes.TASK_USER_TASK -> FlowNodeType.USER_TASK;
      case ActivityTypes.TASK_BUSINESS_RULE -> FlowNodeType.BUSINESS_RULE_TASK;
      case ActivityTypes.TASK_SCRIPT -> FlowNodeType.SCRIPT_TASK;
      case ActivityTypes.TASK_MANUAL_TASK -> FlowNodeType.MANUAL_TASK;
      case ActivityTypes.TASK_RECEIVE_TASK -> FlowNodeType.RECEIVE_TASK;
      case ActivityTypes.TASK_SEND_TASK -> null; // No equivalent in C8
      case ActivityTypes.TASK -> null; // Generic task - no equivalent in C8

      // Gateways
      case ActivityTypes.GATEWAY_EXCLUSIVE -> FlowNodeType.EXCLUSIVE_GATEWAY;
      case ActivityTypes.GATEWAY_PARALLEL -> FlowNodeType.PARALLEL_GATEWAY;
      case ActivityTypes.GATEWAY_INCLUSIVE -> null; // No equivalent in C8
      case ActivityTypes.GATEWAY_COMPLEX -> null; // No equivalent in C8
      case ActivityTypes.GATEWAY_EVENT_BASED -> null; // No equivalent in C8

      // Intermediate Events
      case ActivityTypes.INTERMEDIATE_EVENT_TIMER, ActivityTypes.INTERMEDIATE_EVENT_SIGNAL,
           ActivityTypes.INTERMEDIATE_EVENT_MESSAGE, ActivityTypes.INTERMEDIATE_EVENT_CATCH,
           ActivityTypes.INTERMEDIATE_EVENT_LINK, ActivityTypes.INTERMEDIATE_EVENT_CONDITIONAL ->
          FlowNodeType.INTERMEDIATE_CATCH_EVENT;

      case ActivityTypes.INTERMEDIATE_EVENT_COMPENSATION_THROW, ActivityTypes.INTERMEDIATE_EVENT_THROW,
           ActivityTypes.INTERMEDIATE_EVENT_SIGNAL_THROW, ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW,
           ActivityTypes.INTERMEDIATE_EVENT_NONE_THROW, ActivityTypes.INTERMEDIATE_EVENT_ESCALATION_THROW ->
          FlowNodeType.INTERMEDIATE_THROW_EVENT;

      // Boundary Events
      case ActivityTypes.BOUNDARY_TIMER, ActivityTypes.BOUNDARY_MESSAGE, ActivityTypes.BOUNDARY_SIGNAL,
           ActivityTypes.BOUNDARY_COMPENSATION, ActivityTypes.BOUNDARY_ERROR, ActivityTypes.BOUNDARY_ESCALATION,
           ActivityTypes.BOUNDARY_CANCEL, ActivityTypes.BOUNDARY_CONDITIONAL -> null; // No equivalent in C8

      // Sub Processes and Activities
      case ActivityTypes.SUB_PROCESS -> FlowNodeType.SUB_PROCESS;
      case ActivityTypes.SUB_PROCESS_AD_HOC -> null; // No equivalent in C8
      case ActivityTypes.CALL_ACTIVITY -> FlowNodeType.CALL_ACTIVITY;
      case ActivityTypes.TRANSACTION -> FlowNodeType.SUB_PROCESS; // TODO how to handle this?
      case ActivityTypes.MULTI_INSTANCE_BODY -> FlowNodeType.MULTI_INSTANCE_BODY;
      case ActivityTypes.TASK -> FlowNodeType.TASK;
      default -> throw new IllegalArgumentException("Unknown type: " + activityType);
    };
  }

}
