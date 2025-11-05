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

import io.camunda.db.rdbms.write.domain.DecisionRequirementsDbModel;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.repository.DecisionRequirementsDefinition;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting DecisionRequirementsDefinition to C8 DecisionRequirementsDbModel.
 */
@Component
public class DefaultDecisionRequirementsDefinitionConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(DecisionRequirementsDefinition.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof DecisionRequirementsDefinition c7DecisionRequirements)) {
      return;
    }

    DecisionRequirementsDbModel dbModel = new DecisionRequirementsDbModel.Builder()
        .decisionRequirementsKey(getNextKey())
        .decisionRequirementsId(c7DecisionRequirements.getKey())
        .name(c7DecisionRequirements.getName())
        .resourceName(c7DecisionRequirements.getResourceName())
        .version(c7DecisionRequirements.getVersion())
        .xml(null) // TODO not stored in C7 DecisionRequirementsDefinition
        .tenantId(getTenantId(c7DecisionRequirements.getTenantId()))
        .build();

    // Set the built model in the context
    @SuppressWarnings("unchecked")
    EntityConversionContext<DecisionRequirementsDefinition, DecisionRequirementsDbModel> typedContext =
        (EntityConversionContext<DecisionRequirementsDefinition, DecisionRequirementsDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }
}

