/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.interceptor;

import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static io.camunda.migrator.impl.util.ConverterUtil.getTenantId;

import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting DecisionDefinition to C8 DecisionDefinitionDbModel.
 */
@Component
public class DefaultDecisionDefinitionConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(DecisionDefinition.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof DecisionDefinition c7DecisionDefinition)) {
      return;
    }

    Long decisionRequirementsKey = (Long) context.getMetadata("decisionRequirementsKey");

    DecisionDefinitionDbModel dbModel = new DecisionDefinitionDbModel.DecisionDefinitionDbModelBuilder()
        .decisionDefinitionKey(getNextKey())
        .name(c7DecisionDefinition.getName())
        .decisionDefinitionId(c7DecisionDefinition.getKey())
        .tenantId(getTenantId(c7DecisionDefinition.getTenantId()))
        .version(c7DecisionDefinition.getVersion())
        .decisionRequirementsId(c7DecisionDefinition.getDecisionRequirementsDefinitionKey())
        .decisionRequirementsKey(decisionRequirementsKey)
        .build();

    // Set the built model in the context
    @SuppressWarnings("unchecked")
    EntityConversionContext<DecisionDefinition, DecisionDefinitionDbModel> typedContext =
        (EntityConversionContext<DecisionDefinition, DecisionDefinitionDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }
}

