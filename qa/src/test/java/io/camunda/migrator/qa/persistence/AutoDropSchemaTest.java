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
import java.util.Map;
import javax.sql.DataSource;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@WithMultiDb
public class AutoDropSchemaTest {

  protected static final String MIGRATION_MAPPING_TABLE = "MIGRATION_MAPPING";
  protected SpringApplicationBuilder springApplication;

  @BeforeEach
  void setup() {
    this.springApplication = new SpringApplicationBuilder(MigrationTestApplication.class);
    springApplication.profiles("drop-schema");
  }

  @Test
  void shouldMigrationSchemaBeKeptOnShutdown() throws Exception {
    // given spring application is running with drop-schema flag disabled
    var context = springApplication.run();
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is kept
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isTrue();
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnShutdown() throws Exception {
    // given spring application is running with drop-schema flag enabled
    var context = springApplication.properties(Map.of("camunda.migrator.drop-schema", true)).run();
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isFalse();
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnShutdownWithPrefix() throws Exception {
    // given spring application is running with drop-schema flag enabled
    var context = springApplication.properties(Map.of("camunda.migrator.drop-schema", true, "camunda.migrator.table-prefix", "FOO_")).run();
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, "FOO_" + MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, "FOO_" + MIGRATION_MAPPING_TABLE)).isFalse();
  }

  /**
   * Recreate the schema after it was dropped to allow other tests to run
   * @param durableDataSource
   * @param tablePrefix
   * @throws Exception
   */
  private static void recreateSchema(DataSource durableDataSource, String tablePrefix) throws Exception {
    AbstractConfiguration abstractConfiguration = new AbstractConfiguration();
    MultiTenantSpringLiquibase schema = abstractConfiguration.createSchema(durableDataSource, tablePrefix, "db/changelog/migrator/db.0.0.1.xml");
    schema.afterPropertiesSet();
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, tablePrefix + MIGRATION_MAPPING_TABLE));
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
