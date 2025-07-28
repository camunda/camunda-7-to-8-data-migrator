/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.impl.logging;

import io.camunda.migrator.impl.DateVariableInterceptor;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging utility for DateVariableInterceptor.
 * Contains all log messages and string constants used in DateVariableInterceptor.
 */
public class DateVariableInterceptorLogs {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DateVariableInterceptor.class);

  // DateVariableInterceptor Messages
  public static final String CONVERTING_DATE_VARIABLE = "Converting date variable: {}";
  public static final String CONVERTED_DATE_VARIABLE = "Converted date {} to {}";

  public static void convertingDateVariable(String variableName) {
    LOGGER.info(CONVERTING_DATE_VARIABLE, variableName);
  }

  public static void convertedDateVariable(Date value, String formattedDate) {
    LOGGER.debug(CONVERTED_DATE_VARIABLE, value, formattedDate);
  }
}
