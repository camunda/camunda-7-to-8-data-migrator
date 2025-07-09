/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl;

import io.camunda.migrator.interceptor.VariableInterceptor;
import io.camunda.migrator.interceptor.VariableInvocation;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Interceptor for handling date variables during migration from Camunda 7 to Camunda 8.
 * It formats date variables to a specific string format.
 * <p>
 * The interceptor is ordered with priority 10 to ensure it runs after the default interceptor.
 */
@Order(10)
@Component
public class DateVariableInterceptor implements VariableInterceptor {

  public static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  protected static final Logger LOGGER = LoggerFactory.getLogger(DateVariableInterceptor.class);

  @Override
  public void execute(VariableInvocation invocation) throws Exception {
    VariableInstanceEntity variable = invocation.getC7Variable();
    TypedValue typedValue = variable.getTypedValue(false);

    if (ValueType.DATE.getName().equals(typedValue.getType().getName())) {
      LOGGER.info("Converting date variable: {}", variable.getName());

      Date value = (Date) typedValue.getValue();
      if (value != null) {
        String formattedDate = DATE_FORMAT.format(value);
        invocation.setVariableValue(formattedDate);
        LOGGER.debug("Converted date {} to {}", value, formattedDate);
      }
    }
  }
}