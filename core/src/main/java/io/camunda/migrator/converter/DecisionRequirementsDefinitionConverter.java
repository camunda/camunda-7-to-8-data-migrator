/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static io.camunda.migrator.impl.util.ConverterUtil.getTenantId;

import io.camunda.db.rdbms.write.domain.DecisionRequirementsDbModel;
import org.camunda.bpm.engine.repository.DecisionRequirementsDefinition;

public class DecisionRequirementsDefinitionConverter {

  public DecisionRequirementsDbModel apply(DecisionRequirementsDefinition legacyDecisionRequirements) {
    return new DecisionRequirementsDbModel.Builder()
        .decisionRequirementsKey(getNextKey())
        .decisionRequirementsId(legacyDecisionRequirements.getKey())
        .name(legacyDecisionRequirements.getName())
        .resourceName(legacyDecisionRequirements.getResourceName())
        .version(legacyDecisionRequirements.getVersion())
        .xml(null) // TODO not stored in C7 DecisionRequirementsDefinition
        .tenantId(getTenantId(legacyDecisionRequirements.getTenantId()))
        .build();
  }
}
