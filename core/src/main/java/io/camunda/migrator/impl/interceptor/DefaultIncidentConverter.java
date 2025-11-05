/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.interceptor;

import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;

import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.search.entities.IncidentEntity;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricIncident to C8 IncidentDbModel.
 */
@Component
public class DefaultIncidentConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricIncident.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof HistoricIncident historicIncident)) {
      return;
    }

    // Extract metadata
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");
    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long jobDefinitionKey = (Long) context.getMetadata("jobDefinitionKey");
    Long flowNodeInstanceKey = (Long) context.getMetadata("flowNodeInstanceKey");

    IncidentDbModel dbModel = new IncidentDbModel.Builder()
        .incidentKey(getNextKey())
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(historicIncident.getProcessDefinitionKey())
        .processInstanceKey(processInstanceKey)
        .flowNodeInstanceKey(flowNodeInstanceKey) //TODO: is this linking correct?
        .flowNodeId(historicIncident.getActivityId())
        .jobKey(jobDefinitionKey)
        .errorType(null) // TODO: does error type exist in C7?
        .errorMessage(historicIncident.getIncidentMessage())
        .creationDate(convertDate(historicIncident.getCreateTime()))
        .state(convertState(0)) //TODO: make HistoricIncidentEventEntity#getIncidentState() accessible
        .treePath(null) //TODO ?
        .tenantId(historicIncident.getTenantId())
        .build();

    // Set the built model in the context
    @SuppressWarnings("unchecked")
    EntityConversionContext<HistoricIncident, IncidentDbModel> typedContext =
        (EntityConversionContext<HistoricIncident, IncidentDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }

  private IncidentEntity.IncidentState convertState(Integer state) {
    return switch (state) {
      case 0 -> IncidentEntity.IncidentState.ACTIVE; // open
      case 1, 2 -> IncidentEntity.IncidentState.RESOLVED; // resolved/deleted

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }
}

