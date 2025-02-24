package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import org.camunda.bpm.engine.repository.DecisionDefinition;

import static io.camunda.migrator.history.ConverterUtil.getNextKey;

public class DecisionDefinitionConverter {

  public DecisionDefinitionDbModel apply(DecisionDefinition legacyDecisionDefinition) {

    return new DecisionDefinitionDbModel.DecisionDefinitionDbModelBuilder()
        .decisionDefinitionKey(getNextKey())
        .name(legacyDecisionDefinition.getName())
        .decisionDefinitionId(legacyDecisionDefinition.getId())
        .tenantId(legacyDecisionDefinition.getTenantId())
        .version(legacyDecisionDefinition.getVersion())
        .decisionRequirementsId(legacyDecisionDefinition.getDecisionRequirementsDefinitionId())
        .decisionRequirementsKey(null) //TODO C7 decision requirement key is String ?
        .build();
  }

}
