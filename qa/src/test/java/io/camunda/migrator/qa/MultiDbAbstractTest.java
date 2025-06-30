/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.oracle.OracleContainer;

public class MultiDbAbstractTest {

  public static final int POSTGRESQL_PORT = 55432;
  public static final int ORACLE_PORT = 15432;
  protected static final Logger LOGGER = LoggerFactory.getLogger(MultiDbAbstractTest.class);

  protected static Map<String, JdbcDatabaseContainer<?>> containers = createContainers();
  
  static { // Start db container before initializing Spring context
    startContainer();
  }

  protected static void startContainer() {
    String activeProfile = System.getProperty("spring.profiles.active", "");
    LOGGER.info("Running tests with profile [{}]", activeProfile);
    JdbcDatabaseContainer<?> container = containers.get(activeProfile);
    if (container != null) {
      container.start();
    }
  }

  protected static PostgreSQLContainer<?> createPostgreSQLContainer() {
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("process-engine")
        .withUsername("camunda")
        .withPassword("camunda")
        .withExposedPorts(5432);
    postgres.setPortBindings(List.of(POSTGRESQL_PORT + ":5432"));
    return postgres;
  }

  protected static OracleContainer createOracleContainer() {
    OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:latest")
        .withDatabaseName("ORCLDB")
        .withUsername("camunda")
        .withPassword("camunda")
        .withExposedPorts(1521);
    oracle.setPortBindings(List.of(ORACLE_PORT + ":1521"));
    return oracle;
  }

  protected static Map<String, JdbcDatabaseContainer<?>> createContainers() {
    Map<String, JdbcDatabaseContainer<?>> containers = new HashMap<>();
    containers.put("postgresql", createPostgreSQLContainer());
    containers.put("oracle", createOracleContainer());
    return containers;
  }

}
