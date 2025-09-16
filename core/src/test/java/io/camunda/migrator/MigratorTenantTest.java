/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.config.property.MigratorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "camunda.migrator.tenantIds=tenant1,tenant2, tenant3" })
@SpringBootTest
public class MigratorTenantTest {

  @Autowired
  protected MigratorProperties migratorProperties;

  @Test
  public void shouldSetTenantIds() {
    assertThat(migratorProperties.getTenantIds()).contains("tenant1", "tenant2", "tenant3");
  }

}
