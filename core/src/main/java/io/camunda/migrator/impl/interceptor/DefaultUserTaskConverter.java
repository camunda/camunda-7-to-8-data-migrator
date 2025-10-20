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

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.impl.util.ConverterUtil;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricTaskInstance to C8 UserTaskDbModel properties.
 */
@Component
public class DefaultUserTaskConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricTaskInstance.class);
  }

  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  public void execute(EntityConversionContext context) {
    if (!(context.getC7Entity() instanceof HistoricTaskInstance task)) {
      return;
    }

    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long flowNodeInstanceKey = (Long) context.getMetadata("flowNodeInstanceKey");

    context.setProperty("userTaskKey", getNextKey());
    context.setProperty("elementId", task.getTaskDefinitionKey());
    context.setProperty("processInstanceKey", processInstanceKey);
    context.setProperty("flowNodeInstanceKey", flowNodeInstanceKey);
    context.setProperty("creationDate", convertDate(task.getStartTime()));
    context.setProperty("completionDate", convertDate(task.getEndTime()));
    context.setProperty("assignee", task.getAssignee());
    context.setProperty("state", task.getEndTime() != null ? "COMPLETED" : "CREATED");
    context.setProperty("tenantId", ConverterUtil.getTenantId(task.getTenantId()));
    context.setProperty("partitionId", C7_HISTORY_PARTITION_ID);

    // Optional properties
    if (task.getName() != null) {
      context.setProperty("name", task.getName());
    }
    if (task.getPriority() > 0) {//TODO
      context.setProperty("priority", task.getPriority());
    }
  }
}

