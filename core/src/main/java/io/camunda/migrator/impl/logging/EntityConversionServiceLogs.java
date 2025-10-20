/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging for entity conversion service.
 */
public class EntityConversionServiceLogs {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityConversionServiceLogs.class);

  private EntityConversionServiceLogs() {
    // Utility class
  }

  public static void logExecutingInterceptor(String interceptorName, String entityType) {
    LOGGER.debug("Executing interceptor {} for entity type {}", interceptorName, entityType);
  }

  public static void logInterceptorError(String interceptorName, String entityType) {
    LOGGER.error("Error executing interceptor {} for entity type {}", interceptorName, entityType);
  }

  public static String formatInterceptorError(String interceptorName, String entityType) {
    return String.format("Error executing interceptor %s for entity type %s", interceptorName, entityType);
  }
}