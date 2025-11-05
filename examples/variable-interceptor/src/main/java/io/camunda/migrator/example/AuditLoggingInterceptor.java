/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.example;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example showing how to add custom audit logging for all entity conversions.
 * This is a universal interceptor that handles all entity types.
 */
public class AuditLoggingInterceptor implements EntityInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLoggingInterceptor.class);

  private boolean enableAudit = true;
  private String auditPrefix = "[AUDIT]";

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(); // Empty = handle all entity types
  }


  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!enableAudit) {
      return;
    }

    String entityType = context.getEntityType().getSimpleName();
    Object entity = context.getC7Entity();

    if (entity instanceof HistoricActivityInstance activity) {
      LOGGER.info("{} Converting {} - Activity ID: {}",
          auditPrefix, entityType, activity.getActivityId());
    } else {
      LOGGER.info("{} Converting {} - Entity: {}",
          auditPrefix, entityType, entity.toString());
    }
  }

  public void setEnableAudit(boolean enableAudit) {
    this.enableAudit = enableAudit;
  }

  public void setAuditPrefix(String auditPrefix) {
    this.auditPrefix = auditPrefix;
  }
}
