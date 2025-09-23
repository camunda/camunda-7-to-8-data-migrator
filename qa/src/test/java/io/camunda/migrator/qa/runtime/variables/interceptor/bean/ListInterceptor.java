/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime.variables.interceptor.bean;

import io.camunda.migrator.interceptor.VariableInterceptor;
import io.camunda.migrator.interceptor.VariableInvocation;
import java.util.List;
import java.util.Set;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Order(-1)
public class ListInterceptor implements VariableInterceptor {

  protected final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  protected RuntimeService runtimeService;

  @Override
  public Set<Class<?>> getTypes() {
    return Set.of(ObjectValue.class);
  }

  @Override
  public void execute(VariableInvocation invocation) {
    ObjectValue objectValue = (ObjectValue) invocation.getC7Variable().getTypedValue();
    if (!objectValue.isDeserialized()) {
      try {
        ObjectValue deserializedObjectValue = runtimeService.getVariableTyped(invocation.getC7Variable().getExecutionId(), invocation.getC7Variable().getName(), true);
        List<?> list = (List<?>) deserializedObjectValue.getValue();
        invocation.setVariableValue(MAPPER.writeValueAsString(list));
        // TODO: fix the serialization, C8 creates an incident with message "Expected result of the expression ' namesList' to be 'ARRAY', but was 'STRING'.
        // TODO: Check if the variable comes from the collection of a multi-instance activity in the bpmn model
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
