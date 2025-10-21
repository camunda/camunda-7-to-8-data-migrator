
/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.interceptor;

import static io.camunda.migrator.constants.MigratorConstants.C7_HISTORY_PARTITION_ID;
import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.impl.util.ConverterUtil;
import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState;
import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricActivityInstance (flow nodes) to C8 FlowNodeInstanceDbModel properties.
 * This implements the default conversion logic that was previously in FlowNodeConverter.
 */
@Component
public class DefaultFlowNodeConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricActivityInstance.class);
  }

  @Override
  public int getOrder() {
    return 100; // High priority - runs first
  }

  @Override
  public void execute(EntityConversionContext context) {
    if (!(context.getC7Entity() instanceof HistoricActivityInstance activity)) {
      return;
    }

    // Extract metadata
    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");

    // Set all standard properties
    context.setProperty("flowNodeInstanceKey", getNextKey());
    context.setProperty("flowNodeId", activity.getActivityId());
    context.setProperty("processInstanceKey", processInstanceKey);
    context.setProperty("processDefinitionKey", processDefinitionKey);
    context.setProperty("startDate", convertDate(activity.getStartTime()));
    context.setProperty("endDate", convertDate(activity.getEndTime()));
    context.setProperty("state", convertState(activity));
    context.setProperty("type", convertType(activity.getActivityType()));
    context.setProperty("tenantId", ConverterUtil.getTenantId(activity.getTenantId()));
    context.setProperty("partitionId", C7_HISTORY_PARTITION_ID);

    // Properties not yet migrated from C7
    context.nullifyProperty("incidentKey");
    context.nullifyProperty("treePath");
  }

  private FlowNodeState convertState(HistoricActivityInstance activity) {
    if (activity.getEndTime() != null) {
      return FlowNodeState.COMPLETED;
    }
    return FlowNodeState.ACTIVE;
  }

  private FlowNodeType convertType(String activityType) {
    return switch (activityType) {
      case "startEvent" -> FlowNodeType.START_EVENT;
      case "endEvent" -> FlowNodeType.END_EVENT;
      case "userTask" -> FlowNodeType.USER_TASK;
      case "serviceTask" -> FlowNodeType.SERVICE_TASK;
      case "callActivity" -> FlowNodeType.CALL_ACTIVITY;
      case "subProcess" -> FlowNodeType.SUB_PROCESS;
      case "parallelGateway" -> FlowNodeType.PARALLEL_GATEWAY;
      case "exclusiveGateway" -> FlowNodeType.EXCLUSIVE_GATEWAY;
      default -> FlowNodeType.UNSPECIFIED;
    };
  }
}
