/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import io.camunda.migrator.config.MigratorAutoConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestMigratorAutoConfiguration {

  @Autowired
  protected MigratorAutoConfiguration config;

  @Bean
  @Primary
  public ProcessEngineConfigurationImpl processEngineConfiguration() {
    return config.processEngineConfigurationImpl().setDatabaseSchemaUpdate("create-drop");
  }
}
