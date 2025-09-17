/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.interceptor;

import static io.camunda.migrator.impl.logging.VariableServiceLogs.FILE_TYPE_UNSUPPORTED_ERROR;
import static io.camunda.migrator.impl.logging.VariableServiceLogs.logStartExecution;

import io.camunda.migrator.exception.VariableInterceptorException;
import io.camunda.migrator.interceptor.VariableInterceptor;
import io.camunda.migrator.interceptor.VariableInvocation;
import java.util.Set;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validator for File variable types during migration from Camunda 7 to Camunda 8.
 * <p>
 * Validates that File variables are not used, as they are unsupported in Camunda 8.
 * Can be disabled via unified configuration using the disabled property.
 */
@Order(2)  // Run early - validate file types before transformation
@Component
public class FileVariableValidator implements VariableInterceptor {

  @Override
  public Set<Class<?>> getTypes() {
    return Set.of(FileValue.class); // Only handle FileValue types
  }

  @Override
  public void execute(VariableInvocation invocation) {
    VariableInstanceEntity variable = invocation.getC7Variable();
    logStartExecution(this.getClass(), variable.getName());
    throw new VariableInterceptorException(FILE_TYPE_UNSUPPORTED_ERROR);
  }
}
