/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel.ProcessInstanceDbModelBuilder;
import io.camunda.migrator.impl.EntityConversionService;
import io.camunda.migrator.interceptor.EntityConversionContext;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Refactored ProcessInstanceConverter that uses the EntityInterceptor pattern.
 * <p>
 * This converter delegates the actual conversion logic to configured EntityInterceptors,
 * allowing users to customize the conversion process by:
 * - Providing custom interceptors
 * - Disabling built-in interceptors
 * - Overriding specific properties
 * </p>
 */
public class ProcessInstanceConverterRefactored {
//
//  @Autowired
//  private EntityConversionService entityConversionService;
//
//  public ProcessInstanceDbModel apply(HistoricProcessInstance processInstance,
//                                      Long processDefinitionKey,
//                                      Long parentProcessInstanceKey) {
//    // Create conversion context and store additional context needed for conversion
//    EntityConversionContext<HistoricProcessInstance> context =
//        new EntityConversionContext<>(processInstance, HistoricProcessInstance.class);
//
//    context.setMetadata("processDefinitionKey", processDefinitionKey);
//    context.setMetadata("parentProcessInstanceKey", parentProcessInstanceKey);
//
//    // Execute all interceptors (they will populate the context properties)
//    context = entityConversionService.convertWithContext(context);
//
//    // Build the C8 model from the context properties
//    return buildFromContext(context);
//  }
//
//  /**
//   * Builds a ProcessInstanceDbModel from the conversion context properties.
//   *
//   * @param context the conversion context with all properties set
//   * @return the C8 ProcessInstanceDbModel
//   */
//  private ProcessInstanceDbModel buildFromContext(EntityConversionContext<HistoricProcessInstance> context) {
//    ProcessInstanceDbModelBuilder builder = new ProcessInstanceDbModelBuilder();
//
//    // Map all properties from context to builder
//    if (context.hasProperty("processInstanceKey")) {
//      builder.processInstanceKey((Long) context.getProperty("processInstanceKey"));
//    }
//    if (context.hasProperty("processDefinitionKey")) {
//      builder.processDefinitionKey((Long) context.getProperty("processDefinitionKey"));
//    }
//    if (context.hasProperty("processDefinitionId")) {
//      builder.processDefinitionId((String) context.getProperty("processDefinitionId"));
//    }
//    if (context.hasProperty("startDate")) {
//      builder.startDate((java.time.OffsetDateTime) context.getProperty("startDate"));
//    }
//    if (context.hasProperty("endDate")) {
//      builder.endDate((java.time.OffsetDateTime) context.getProperty("endDate"));
//    }
//    if (context.hasProperty("state")) {
//      builder.state((io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState) context.getProperty("state"));
//    }
//    if (context.hasProperty("tenantId")) {
//      builder.tenantId((String) context.getProperty("tenantId"));
//    }
//    if (context.hasProperty("version")) {
//      builder.version((Integer) context.getProperty("version"));
//    }
//    if (context.hasProperty("parentProcessInstanceKey")) {
//      builder.parentProcessInstanceKey((Long) context.getProperty("parentProcessInstanceKey"));
//    }
//    if (context.hasProperty("parentElementInstanceKey")) {
//      builder.parentElementInstanceKey((Long) context.getProperty("parentElementInstanceKey"));
//    }
//    if (context.hasProperty("treePath")) {
//      builder.treePath((String) context.getProperty("treePath"));
//    }
//    if (context.hasProperty("partitionId")) {
//      builder.partitionId((Integer) context.getProperty("partitionId"));
//    }
//    if (context.hasProperty("historyCleanupDate")) {
//      builder.historyCleanupDate((java.time.OffsetDateTime) context.getProperty("historyCleanupDate"));
//    }
//
//    return builder.build();
//  }
}