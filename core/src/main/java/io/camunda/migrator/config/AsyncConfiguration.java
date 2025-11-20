/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config;

import io.camunda.migrator.impl.logging.ConfigurationLogs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ConditionalOnProperty(name = "camunda.migrator.async.enabled", havingValue = "true")
@EnableAsync
public class AsyncConfiguration {

  public AsyncConfiguration() {
    ConfigurationLogs.logAsyncEnabled();
  }
}
