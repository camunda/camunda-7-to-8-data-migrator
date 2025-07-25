/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.impl.logging;

import io.camunda.migrator.impl.DefaultVariableInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging utility for DefaultVariableInterceptor.
 * Contains all log messages and string constants used in DefaultVariableInterceptor.
 */
public class DefaultVariableInterceptorLogs {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultVariableInterceptor.class);

  // DefaultVariableInterceptor Messages
  private static final String START_EXECUTION = "Start {} execution for variable: {}";
  private static final String END_EXECUTION = "End {} execution for variable: {}";

  public static void startExecution(String variableName) {
    LOGGER.debug(START_EXECUTION, DefaultVariableInterceptor.class, variableName);
  }

  public static void endExecution(String variableName) {
    LOGGER.debug(END_EXECUTION, DefaultVariableInterceptor.class, variableName);
  }
}
