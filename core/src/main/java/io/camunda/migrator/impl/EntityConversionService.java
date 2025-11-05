/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl;

import io.camunda.migrator.exception.EntityInterceptorException;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.interceptor.EntityTypeDetector;
import io.camunda.migrator.impl.logging.EntityConversionServiceLogs;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing entity conversion using interceptors.
 * This service executes configured entity interceptors in order,
 * allowing them to transform C7 entities to C8 models.
 */
@Service
public class EntityConversionService {

  @Autowired(required = false)
  protected List<EntityInterceptor> configuredEntityInterceptors;

  /**
   * Converts a C7 entity using a provided context by executing all applicable interceptors.
   *
   * @param context the conversion context with entity and metadata already set
   * @param <C7>    the C7 entity type
   * @param <C8>    the C8 database model type
   * @return the conversion context with C8 database model set by interceptors
   */
  public <C7, C8> EntityConversionContext<C7, C8> convertWithContext(EntityConversionContext<C7, C8> context) {
    if (hasInterceptors()) {
      // Filter interceptors by entity type
      List<EntityInterceptor> applicableInterceptors = configuredEntityInterceptors.stream()
          .filter(interceptor -> EntityTypeDetector.supportsEntityType(interceptor, context.getEntityType()))
          .toList();

      // Execute each interceptor
      for (EntityInterceptor interceptor : applicableInterceptors) {
        executeInterceptor(interceptor, context);
      }
    }

    return context;
  }

  /**
   * Converts a C7 entity to C8 database model by executing all applicable interceptors.
   *
   * @param c7Entity   the C7 historic entity
   * @param entityType the entity type class
   * @param <C7>       the C7 entity type
   * @param <C8>       the C8 database model type
   * @return the conversion context with C8 database model set
   */
  public <C7, C8> EntityConversionContext<C7, C8> convert(C7 c7Entity, Class<?> entityType) {
    EntityConversionContext<C7, C8> context = new EntityConversionContext<>(c7Entity, entityType);
    return convertWithContext(context);
  }

  /**
   * Executes a single interceptor on the conversion context.
   *
   * @param interceptor the interceptor to execute
   * @param context     the conversion context
   */
  private void executeInterceptor(EntityInterceptor interceptor, EntityConversionContext<?, ?> context) {
    try {
      EntityConversionServiceLogs.logExecutingInterceptor(interceptor.getClass().getSimpleName(),
          context.getEntityType().getSimpleName());
      interceptor.execute(context);
    } catch (Exception ex) {
      String interceptorName = interceptor.getClass().getSimpleName();
      String entityType = context.getEntityType().getSimpleName();
      EntityConversionServiceLogs.logInterceptorError(interceptorName, entityType);

      if (ex instanceof EntityInterceptorException) {
        throw ex;
      } else {
        throw new EntityInterceptorException(
            EntityConversionServiceLogs.formatInterceptorError(interceptorName, entityType), ex);
      }
    }
  }

  /**
   * Checks if there are any configured entity interceptors.
   *
   * @return true if interceptors are configured, false otherwise
   */
  private boolean hasInterceptors() {
    return configuredEntityInterceptors != null && !configuredEntityInterceptors.isEmpty();
  }
}