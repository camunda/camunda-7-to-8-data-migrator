/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
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
    EntityConversionContext<HistoricActivityInstance, FlowNodeInstanceDbModel> context =
        new EntityConversionContext<>(activity, HistoricActivityInstance.class);

    context.setMetadata("processInstanceKey", processInstanceKey);
    context.setMetadata("processDefinitionKey", processDefinitionKey);

    // Execute all interceptors - they will build the FlowNodeInstanceDbModel
    context = entityConversionService.convertWithContext(context);

    // Return the built C8 model
    return context.getC8DbModel();
  }
}

