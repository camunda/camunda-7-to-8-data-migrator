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
import static io.camunda.migrator.impl.util.ConverterUtil.getTenantId;

import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.List;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricDecisionInputInstance;
import org.camunda.bpm.engine.history.HistoricDecisionInstance;
import org.camunda.bpm.engine.history.HistoricDecisionOutputInstance;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricDecisionInstance to C8 DecisionInstanceDbModel.
 */
@Component
public class DefaultDecisionInstanceConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricDecisionInstance.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof HistoricDecisionInstance decisionInstance)) {
      return;
    }

    // Extract metadata
    Long decisionDefinitionKey = (Long) context.getMetadata("decisionDefinitionKey");
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");
    Long decisionRequirementsDefinitionKey = (Long) context.getMetadata("decisionRequirementsDefinitionKey");
    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long rootDecisionDefinitionKey = (Long) context.getMetadata("rootDecisionDefinitionKey");
    Long flowNodeInstanceKey = (Long) context.getMetadata("flowNodeInstanceKey");
    String flowNodeId = (String) context.getMetadata("flowNodeId");

    Long decisionInstanceKey = getNextKey();
    DecisionInstanceDbModel dbModel = new DecisionInstanceDbModel.Builder()
        .partitionId(C7_HISTORY_PARTITION_ID)
        .decisionInstanceId(String.format("%d-%s", decisionInstanceKey, decisionInstance.getId()))
        .decisionInstanceKey(decisionInstanceKey)
        .evaluationDate(convertDate(decisionInstance.getEvaluationTime()))
        .evaluationFailure(null) // not stored in HistoricDecisionInstance
        .evaluationFailureMessage(null) // not stored in HistoricDecisionInstance
        .result(String.valueOf(decisionInstance.getCollectResultValue()))
        .flowNodeInstanceKey(flowNodeInstanceKey)
        .flowNodeId(flowNodeId)
        .processInstanceKey(processInstanceKey)
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(decisionInstance.getProcessDefinitionKey())
        .decisionDefinitionKey(decisionDefinitionKey)
        .decisionDefinitionId(decisionInstance.getDecisionDefinitionKey())
        .decisionRequirementsKey(decisionRequirementsDefinitionKey)
        .decisionRequirementsId(decisionInstance.getDecisionRequirementsDefinitionKey())
        .rootDecisionDefinitionKey(rootDecisionDefinitionKey)
        .tenantId(getTenantId(decisionInstance.getTenantId()))
        .evaluatedInputs(mapInputs(decisionInstance.getId(), decisionInstance.getInputs()))
        .evaluatedOutputs(mapOutputs(decisionInstance.getId(), decisionInstance.getOutputs()))
        .historyCleanupDate(convertDate(decisionInstance.getRemovalTime()))
        .build();

    // Set the built model in the context
    @SuppressWarnings("unchecked")
    EntityConversionContext<HistoricDecisionInstance, DecisionInstanceDbModel> typedContext =
        (EntityConversionContext<HistoricDecisionInstance, DecisionInstanceDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }

  private List<DecisionInstanceDbModel.EvaluatedInput> mapInputs(String decisionInstanceId,
                                                                  List<HistoricDecisionInputInstance> c7Inputs) {
    return c7Inputs.stream().map(input -> new DecisionInstanceDbModel.EvaluatedInput(decisionInstanceId,
        input.getId(),
        input.getClauseName(),
        String.valueOf(input.getValue())
    )).toList();
  }

  private List<DecisionInstanceDbModel.EvaluatedOutput> mapOutputs(String decisionInstanceId,
                                                                    List<HistoricDecisionOutputInstance> c7Outputs) {
    return c7Outputs.stream().map(output -> new DecisionInstanceDbModel.EvaluatedOutput(decisionInstanceId,
        output.getId(),
        output.getClauseName(),
        String.valueOf(output.getValue()),
        output.getRuleId(),
        output.getRuleOrder())).toList();
  }
}

