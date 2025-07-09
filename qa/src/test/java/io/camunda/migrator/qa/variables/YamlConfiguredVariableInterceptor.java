/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.variables;

import io.camunda.migrator.interceptor.VariableInterceptor;
import io.camunda.migrator.interceptor.VariableInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test variable interceptor that is configured via YAML instead of Spring annotations.
 * This interceptor demonstrates the plugin system capability to load interceptors
 * from YAML configuration without requiring @Component annotations.
 */
public class YamlConfiguredVariableInterceptor implements VariableInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(YamlConfiguredVariableInterceptor.class);

  // Configurable properties that can be set via YAML
  private String prefix = "YAML_";
  private String logMessage = "Hello from YAML interceptor";
  private boolean enableTransformation = true;
  private String targetVariable = "yamlVar";

  @Override
  public void execute(VariableInvocation invocation) throws Exception {
    LOGGER.debug("Start {} execution for variable: {}", YamlConfiguredVariableInterceptor.class,
        invocation.getC7Variable().getName());

    String variableName = invocation.getC7Variable().getName();

    if (targetVariable.equals(variableName)) {
      LOGGER.info(logMessage);

      if (enableTransformation) {
        String originalValue = String.valueOf(invocation.getC7Variable().getValue());
        String transformedValue = prefix + originalValue;
        invocation.setVariableValue(transformedValue);
        LOGGER.info("Transformed variable {} from '{}' to '{}'", variableName, originalValue, transformedValue);
      }
    }

    // Handle exception testing scenario
    if ("yamlExFlag".equals(variableName)) {
      if (Boolean.valueOf(invocation.getC7Variable().getValue().toString()) == true) {
        throw new RuntimeException("Expected exception from YAML interceptor");
      } else {
        LOGGER.info("Success from YAML interceptor");
      }
    }

    LOGGER.debug("End {} execution for variable: {}", YamlConfiguredVariableInterceptor.class,
        invocation.getC7Variable().getName());
  }

  // Setter methods for YAML configuration
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public void setEnableTransformation(boolean enableTransformation) {
    this.enableTransformation = enableTransformation;
  }

  public void setTargetVariable(String targetVariable) {
    this.targetVariable = targetVariable;
  }

  // Getter methods for testing
  public String getPrefix() {
    return prefix;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public boolean isEnableTransformation() {
    return enableTransformation;
  }

  public String getTargetVariable() {
    return targetVariable;
  }
}
