
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

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.impl.util.ConverterUtil;
import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState;
import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricActivityInstance (flow nodes) to C8 FlowNodeInstanceDbModel.
 * This implements the default conversion logic that was previously in FlowNodeConverter.
 */
@Component
public class DefaultFlowNodeConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricActivityInstance.class);
  }


  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof HistoricActivityInstance activity)) {
      return;
    }

    // Extract metadata
    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");

    // Build the FlowNodeInstanceDbModel
    FlowNodeInstanceDbModel dbModel = new FlowNodeInstanceDbModelBuilder()
        .flowNodeInstanceKey(getNextKey())
        .flowNodeId(activity.getActivityId())
        .processInstanceKey(processInstanceKey)
        .processDefinitionKey(processDefinitionKey)
        .startDate(convertDate(activity.getStartTime()))
        .endDate(convertDate(activity.getEndTime()))
        .state(convertState(activity))
        .type(convertType(activity.getActivityType()))
        .tenantId(ConverterUtil.getTenantId(activity.getTenantId()))
        .partitionId(C7_HISTORY_PARTITION_ID)
        // Properties not yet migrated from C7
        .incidentKey(null)
        .treePath(null)
        .build();

    // Set the built model in the context
    context.setC8DbModel(dbModel);
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
