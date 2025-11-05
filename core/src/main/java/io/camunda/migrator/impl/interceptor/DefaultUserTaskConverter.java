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

import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.impl.util.ConverterUtil;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricTaskInstance to C8 UserTaskDbModel.
 */
@Component
public class DefaultUserTaskConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricTaskInstance.class);
  }


  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof HistoricTaskInstance task)) {
      return;
    }

    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long flowNodeInstanceKey = (Long) context.getMetadata("flowNodeInstanceKey");
    Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");
    Integer processDefinitionVersion = (Integer) context.getMetadata("processDefinitionVersion");

    // Build the UserTaskDbModel
    UserTaskDbModel.Builder builder = new UserTaskDbModel.Builder()
        .userTaskKey(getNextKey())
        .elementId(task.getTaskDefinitionKey())
        .processInstanceKey(processInstanceKey)
        .elementInstanceKey(flowNodeInstanceKey)
        .creationDate(convertDate(task.getStartTime()))
        .completionDate(convertDate(task.getEndTime()))
        .assignee(task.getAssignee())
        .state(convertState(task))
        .tenantId(ConverterUtil.getTenantId(task.getTenantId()))
        .partitionId(C7_HISTORY_PARTITION_ID)
        .processDefinitionId(task.getProcessDefinitionKey())
        .processDefinitionKey(processDefinitionKey)
        .dueDate(convertDate(task.getDueDate()))
        .followUpDate(convertDate(task.getFollowUpDate()))
        .priority(task.getPriority())
        .historyCleanupDate(convertDate(task.getRemovalTime()));

    // Optional properties
    if (task.getName() != null) {
      builder.name(task.getName());
    }
    if (processDefinitionVersion != null) {
      builder.processDefinitionVersion(processDefinitionVersion);
    }

    // Properties not yet migrated
    builder.formKey(null)
        .candidateGroups(null)
        .candidateUsers(null)
        .externalFormReference(null)
        .customHeaders(null);

    // Set the built model in the context
    context.setC8DbModel(builder.build());
  }

  private UserTaskDbModel.UserTaskState convertState(HistoricTaskInstance task) {
    if (task.getEndTime() != null) {
      return task.getDeleteReason() != null
          ? UserTaskDbModel.UserTaskState.CANCELED
          : UserTaskDbModel.UserTaskState.COMPLETED;
    }
    return UserTaskDbModel.UserTaskState.CREATED;
  }
}

