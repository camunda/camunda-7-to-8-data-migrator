
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
import static io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState;

import io.camunda.migrator.constants.MigratorConstants;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.impl.util.ConverterUtil;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricProcessInstance to C8 ProcessInstanceDbModel properties.
 * This implements the default conversion logic that was previously in ProcessInstanceConverter.
 * <p>
 * Users can disable this via configuration and provide their own implementation, or
 * add additional interceptors that run after this one to override specific properties.
 * </p>
 */
@Order(100)  // High priority - runs first to set default values
@Component
public class DefaultProcessInstanceConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricProcessInstance.class);
  }

  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  public void execute(EntityConversionContext context) {
    if (!(context.getC7Entity() instanceof HistoricProcessInstance processInstance)) {
      return;
    }

    // Extract additional context from metadata
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");
    Long parentProcessInstanceKey = (Long) context.getMetadata("parentProcessInstanceKey");

    // Set all standard properties
    context.setProperty("processInstanceKey", getNextKey());
    context.setProperty("processDefinitionKey", processDefinitionKey);
    context.setProperty("processDefinitionId", processInstance.getProcessDefinitionKey());
    context.setProperty("startDate", convertDate(processInstance.getStartTime()));
    context.setProperty("endDate", convertDate(processInstance.getEndTime()));
    context.setProperty("state", convertState(processInstance.getState()));
    context.setProperty("tenantId", getTenantId(processInstance));
    context.setProperty("version", processInstance.getProcessDefinitionVersion());
    context.setProperty("parentProcessInstanceKey", parentProcessInstanceKey);
    context.setProperty("partitionId", C7_HISTORY_PARTITION_ID);
    context.setProperty("historyCleanupDate", convertDate(processInstance.getRemovalTime()));

    // Properties that are not yet migrated from C7 - set to null explicitly
    context.nullifyProperty("parentElementInstanceKey");
    context.nullifyProperty("treePath");
  }

  protected ProcessInstanceState convertState(String state) {
    return switch (state) {
      case "ACTIVE", "SUSPENDED" -> ProcessInstanceState.ACTIVE;
      case "COMPLETED" -> ProcessInstanceState.COMPLETED;
      case "EXTERNALLY_TERMINATED", "INTERNALLY_TERMINATED" -> ProcessInstanceState.CANCELED;
      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }

  protected String getTenantId(HistoricProcessInstance processInstance) {
    return processInstance != null
        ? ConverterUtil.getTenantId(processInstance.getTenantId())
        : MigratorConstants.C8_DEFAULT_TENANT;
  }
}
