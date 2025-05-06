/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
/*package io.camunda.migrator;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.search.query.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MigrationCountTest {

  @Autowired
  private RdbmsService rdbmsService;

  @Test
  public void shouldMigrateProcessInstances() {
    long processInstances = rdbmsService.getProcessInstanceReader().search(new ProcessInstanceQuery.Builder().build())
        .total();

    assertThat(processInstances).isEqualTo(3);
  }
}*/

