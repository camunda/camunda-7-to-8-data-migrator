/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;

import com.zaxxer.hikari.HikariDataSource;
import io.camunda.migrator.config.mybatis.AbstractConfiguration;
import io.camunda.migrator.qa.MigrationTestApplication;
import io.camunda.migrator.qa.util.WithMultiDb;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@WithMultiDb
public class AutoDropSchemaTest {

  protected static final String MIGRATION_MAPPING_TABLE = "MIGRATION_MAPPING";
  protected SpringApplication springApplication;

  @BeforeEach
  void setup() {
    springApplication = new SpringApplication(MigrationTestApplication.class);
    springApplication.setAdditionalProfiles("auto-drop");
  }

  @Test
  void shouldMigrationSchemaBeKeptOnShutdown() throws SQLException {
    // given spring application is running with auto-drop disabled
    ConfigurableApplicationContext context = springApplication.run();
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is kept
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isTrue();
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnShutdown() throws Exception {
    // given spring application is running with auto-drop enabled
    ConfigurableApplicationContext context = springApplication.run("--camunda.migrator.auto-drop=true");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isFalse();
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnShutdownWithPrefix() throws Exception {
    // given spring application is running with auto-drop enabled
    ConfigurableApplicationContext context = springApplication.run("--camunda.migrator.auto-drop=true", "--camunda.migrator.table-prefix=FOO_");
    DataSource migratorDataSource = (DataSource) context.getBean("migratorDataSource");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(migratorDataSource, "FOO_" + MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, "FOO_" + MIGRATION_MAPPING_TABLE)).isFalse();
  }

  /**
   * Create a new DataSource with the same configuration as the migratorDataSource bean to check if the
   * table still exists after the application context is closed.
   */
  private static DataSource createDurableDataSource(ConfigurableApplicationContext context) {
    HikariDataSource durableDataSource = new HikariDataSource();
    durableDataSource.setJdbcUrl(context.getEnvironment().getProperty("camunda.migrator.c7.data-source.jdbc-url"));
    durableDataSource.setUsername(context.getEnvironment().getProperty("camunda.migrator.c7.data-source.username"));
    durableDataSource.setPassword(context.getEnvironment().getProperty("camunda.migrator.c7.data-source.password"));
    return durableDataSource;
  }

  private static boolean tableExists(DataSource dataSource, String tableName) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      String schema = conn.getSchema();
      String dbVendor = meta.getDatabaseProductName().toLowerCase();

      String lookupName = tableName;
      if (dbVendor.contains("postgres")) {
        lookupName = tableName.toLowerCase();
      } else if (dbVendor.contains("oracle")) {
        lookupName = tableName.toUpperCase();
      } else if (dbVendor.contains("h2")) {
        lookupName = tableName.toUpperCase();
      }

      try (ResultSet rs = meta.getTables(conn.getCatalog(), schema, lookupName, new String[]{"TABLE"})) {
        return rs.next();
      }
    }
  }
}
