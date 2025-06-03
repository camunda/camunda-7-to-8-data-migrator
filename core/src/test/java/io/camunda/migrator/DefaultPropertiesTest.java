/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DefaultPropertiesTest {

  @Autowired
  private RuntimeMigrator runtimeMigrator;

  @Autowired
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Test
  public void shouldHaveDefaultBatchSize() {
    Assertions.assertThat(runtimeMigrator.batchSize).isEqualTo(500);
  }

  @Test
  public void shouldHaveDisabledJobExecutor() {
    Assertions.assertThat(processEngineConfiguration.getJobExecutor().isActive()).isEqualTo(false);
    Assertions.assertThat(processEngineConfiguration.getJobExecutor().isActive()).isEqualTo(false);
  }
}