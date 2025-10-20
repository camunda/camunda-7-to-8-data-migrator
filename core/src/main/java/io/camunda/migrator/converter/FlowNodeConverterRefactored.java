/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder;
import io.camunda.migrator.impl.EntityConversionService;
import io.camunda.migrator.interceptor.EntityConversionContext;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Refactored FlowNodeConverter that uses the EntityInterceptor pattern.
 * Note: This is an example showing how to refactor other converters.
 * The actual FlowNodeConverter is still using the old pattern.
 */
public class FlowNodeConverterRefactored {

  @Autowired
  private EntityConversionService entityConversionService;

  public FlowNodeInstanceDbModel apply(HistoricActivityInstance activity,
                                       Long processInstanceKey,
                                       Long processDefinitionKey) {
    // Create context with metadata
    EntityConversionContext<HistoricActivityInstance> context =
        new EntityConversionContext<>(activity, HistoricActivityInstance.class);

    context.setMetadata("processInstanceKey", processInstanceKey);
    context.setMetadata("processDefinitionKey", processDefinitionKey);

    // Execute all interceptors
    context = entityConversionService.convertWithContext(context);

    // Build C8 model from context
    return buildFromContext(context);
  }

  private FlowNodeInstanceDbModel buildFromContext(EntityConversionContext<HistoricActivityInstance> context) {
    FlowNodeInstanceDbModelBuilder builder = new FlowNodeInstanceDbModelBuilder();

    if (context.hasProperty("flowNodeInstanceKey")) {
      builder.flowNodeInstanceKey((Long) context.getProperty("flowNodeInstanceKey"));
    }
    if (context.hasProperty("flowNodeId")) {
      builder.flowNodeId((String) context.getProperty("flowNodeId"));
    }
    if (context.hasProperty("processInstanceKey")) {
      builder.processInstanceKey((Long) context.getProperty("processInstanceKey"));
    }
    if (context.hasProperty("processDefinitionKey")) {
      builder.processDefinitionKey((Long) context.getProperty("processDefinitionKey"));
    }
    if (context.hasProperty("startDate")) {
      builder.startDate((java.time.OffsetDateTime) context.getProperty("startDate"));
    }
    if (context.hasProperty("endDate")) {
      builder.endDate((java.time.OffsetDateTime) context.getProperty("endDate"));
    }
    if (context.hasProperty("state")) {
      builder.state((io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState) context.getProperty("state"));
    }
    if (context.hasProperty("type")) {
      builder.type((io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType) context.getProperty("type"));
    }
    if (context.hasProperty("tenantId")) {
      builder.tenantId((String) context.getProperty("tenantId"));
    }
    if (context.hasProperty("incidentKey")) {
      builder.incidentKey((Long) context.getProperty("incidentKey"));
    }
    if (context.hasProperty("treePath")) {
      builder.treePath((String) context.getProperty("treePath"));
    }
    if (context.hasProperty("partitionId")) {
      builder.partitionId((Integer) context.getProperty("partitionId"));
    }

    return builder.build();
  }
}

