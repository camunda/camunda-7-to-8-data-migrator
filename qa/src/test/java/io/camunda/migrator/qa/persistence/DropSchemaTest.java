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
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.qa.MigrationTestApplication;
import io.camunda.migrator.qa.util.WithMultiDb;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@WithMultiDb
public class DropSchemaTest {

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
    String prefix = "FOO_";
    var context = springApplication.properties(Map.of("camunda.migrator.table-prefix", prefix)).run("--drop-schema");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, prefix + MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, prefix + MIGRATION_MAPPING_TABLE)).isFalse();
  }

  @Test
  void shouldMigrationSchemaBeKeptOnSkippedEntities() throws Exception {
    // given spring application is running with drop-schema flag enabled
    var context = springApplication.run("--drop-schema");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));
    DbClient dbClient = context.getBean("dbClient", DbClient.class);
    // and some entities are marked as skipped
    dbClient.insert("legacyId", null, IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);

    // when application is shut down
    context.close();

    // then migration schema is kept
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isTrue();

    // cleanup db after text
    clearMigrationMappingTable(durableDataSource, "");
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnSkippedEntitiesWithForceFlag() throws Exception {
    // given spring application is running with drop-schema and force flags enabled
    String prefix = "BAR_";
    var context = springApplication.properties(Map.of("camunda.migrator.table-prefix", prefix)).run("--drop-schema", "--force");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, prefix + MIGRATION_MAPPING_TABLE));
    DbClient dbClient = context.getBean("dbClient", DbClient.class);
    // and some entities are marked as skipped
    dbClient.insert("legacyId", null, IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, prefix + MIGRATION_MAPPING_TABLE)).isFalse();
  }

  @Test
  void shouldMigrationSchemaBeDroppedOnShutdownWithoutPrefix() throws Exception {
    // given spring application is running with drop-schema flag enabled
    var context = springApplication.run("--drop-schema");
    DataSource durableDataSource = createDurableDataSource(context);
    ensureTrue("Migration mapping table does not exist", tableExists(durableDataSource, MIGRATION_MAPPING_TABLE));

    // when application is shut down
    context.close();

    // then migration schema is dropped
    assertThat(tableExists(durableDataSource, MIGRATION_MAPPING_TABLE)).isFalse();
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

  public static void clearMigrationMappingTable(DataSource dataSource, String prefix) {
    String sql = "DELETE FROM " + prefix + MIGRATION_MAPPING_TABLE;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to clear table: " + prefix + MIGRATION_MAPPING_TABLE, e);
    }
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
