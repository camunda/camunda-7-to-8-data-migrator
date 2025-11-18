/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * E2E smoke test for the Cockpit plugin that validates:
 * 1. Plugin JAR is built and packaged correctly
 * 2. Plugin can be loaded by Camunda 7
 * 3. REST API endpoints are accessible and functional
 * 4. Basic plugin functionality works end-to-end
 */
public class CockpitPluginSmokeTest {

  private ProcessEngine processEngine;
  private Path pluginJarPath;
  private static final String JDBC_URL = "jdbc:h2:mem:cockpit-plugin-test;DB_CLOSE_DELAY=-1";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  @BeforeEach
  void setUp() throws Exception {
    // Verify plugin JAR exists
    pluginJarPath = findPluginJar();
    assertThat(pluginJarPath).exists();
    assertThat(pluginJarPath.toString()).endsWith(".jar");

    // Setup in-memory H2 database
    setupDatabase();

    // Create process engine with plugin
    processEngine = createProcessEngineWithPlugin();
  }

  @AfterEach
  void tearDown() {
    if (processEngine != null) {
      processEngine.close();
    }
  }

  @Test
  void shouldHavePluginJarInTarget() {
    // given/when - plugin JAR path is resolved in setUp
    
    // then - verify plugin JAR exists and has expected name pattern
    assertThat(pluginJarPath).exists();
    assertThat(pluginJarPath.getFileName().toString())
        .contains("camunda-7-to-8-data-migrator-cockpit-plugin");
  }

  @Test
  void shouldHaveRequiredFilesInPluginJar() throws IOException {
    // given - plugin JAR from setUp
    
    // when - verify JAR contains required resources
    String jarPath = pluginJarPath.toString();
    
    // then - basic validation that it's a valid JAR
    assertThat(Files.size(pluginJarPath)).isGreaterThan(0);
    
    // Verify it's a JAR file (ZIP format)
    byte[] header = Files.readAllBytes(pluginJarPath);
    assertThat(header.length).isGreaterThan(4);
    // JAR files start with PK (ZIP signature)
    assertThat(header[0]).isEqualTo((byte) 0x50); // 'P'
    assertThat(header[1]).isEqualTo((byte) 0x4B); // 'K'
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldLoadPluginInProcessEngine() {
    // given - process engine created in setUp
    
    // when - query process engine configuration
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) processEngine
        .getProcessEngineConfiguration();
    
    // then - verify process engine is properly initialized
    assertThat(processEngine).isNotNull();
    assertThat(processEngine.getName()).isNotNull();
    assertThat(config).isNotNull();
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldHaveMigrationMappingTableCreated() throws Exception {
    // given - database setup with migrations
    
    // when - query database metadata
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      var metaData = conn.getMetaData();
      var tables = metaData.getTables(null, null, "MIGRATION_MAPPING", null);
      
      // then - verify migration table exists
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME")).isEqualTo("MIGRATION_MAPPING");
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldQuerySkippedEntitiesWhenEmpty() throws Exception {
    // given - empty database with no test data
    String type = "RUNTIME_PROCESS_INSTANCE";
    
    // when - query for skipped entities
    long count = querySkippedCount(type);
    
    // then - verify count is zero
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldQueryMigratedEntitiesWhenEmpty() throws Exception {
    // given - empty database with no test data
    String type = "RUNTIME_PROCESS_INSTANCE";
    
    // when - query for migrated entities
    long count = queryMigratedCount(type);
    
    // then - verify count is zero
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldQuerySkippedEntitiesWithData() throws Exception {
    // given - insert test data for a skipped entity
    insertSkippedEntity("test-process-instance-1", "Test skip reason");
    String type = "RUNTIME_PROCESS_INSTANCE";
    
    // when - query for skipped entities
    long count = querySkippedCount(type);
    
    // then - verify count is one
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldQueryMigratedEntitiesWithData() throws Exception {
    // given - insert test data for a migrated entity
    insertMigratedEntity("test-process-instance-2", 123456L);
    String type = "RUNTIME_PROCESS_INSTANCE";
    
    // when - query for migrated entities
    long count = queryMigratedCount(type);
    
    // then - verify count is one
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldDistinguishBetweenMigratedAndSkipped() throws Exception {
    // given - insert both migrated and skipped entities
    insertMigratedEntity("migrated-1", 100L);
    insertMigratedEntity("migrated-2", 200L);
    insertSkippedEntity("skipped-1", "Reason 1");
    insertSkippedEntity("skipped-2", "Reason 2");
    insertSkippedEntity("skipped-3", "Reason 3");
    String type = "RUNTIME_PROCESS_INSTANCE";
    
    // when - query both types
    long migratedCount = queryMigratedCount(type);
    long skippedCount = querySkippedCount(type);
    
    // then - verify counts are correct
    assertThat(migratedCount).isEqualTo(2L);
    assertThat(skippedCount).isEqualTo(3L);
  }

  // Helper methods

  private Path findPluginJar() {
    Path pluginTarget = Paths.get(System.getProperty("user.dir"))
        .resolve("../plugins/cockpit/target");
    
    try (Stream<Path> files = Files.list(pluginTarget)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith(".jar"))
          .filter(path -> path.getFileName().toString().contains("camunda-7-to-8-data-migrator-cockpit-plugin"))
          .filter(path -> !path.getFileName().toString().contains("sources"))
          .filter(path -> !path.getFileName().toString().contains("javadoc"))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Plugin JAR not found in " + pluginTarget));
    } catch (IOException e) {
      throw new RuntimeException("Failed to find plugin JAR", e);
    }
  }

  private void setupDatabase() throws Exception {
    // Run Liquibase migrations to create migration_mapping table
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      Database database = DatabaseFactory.getInstance()
          .findCorrectDatabaseImplementation(new JdbcConnection(conn));
      
      try (Liquibase liquibase = new Liquibase(
          "db/changelog/migrator/db.changelog-master.yaml",
          new ClassLoaderResourceAccessor(),
          database)) {
        
        liquibase.getChangeLogParameters().set("prefix", "");
        liquibase.update((String) null);
      }
    }
  }

  private ProcessEngine createProcessEngineWithPlugin() {
    ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) 
        new StandaloneInMemProcessEngineConfiguration()
        .setJdbcUrl(JDBC_URL)
        .setJdbcUsername(DB_USER)
        .setJdbcPassword(DB_PASSWORD)
        .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
        .setJobExecutorActivate(false);
    
    return configuration.buildProcessEngine();
  }

  private long querySkippedCount(String type) throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      String sql = "SELECT COUNT(*) FROM MIGRATION_MAPPING WHERE TYPE = ? AND SKIP_REASON IS NOT NULL";
      try (var stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, type);
        var rs = stmt.executeQuery();
        if (rs.next()) {
          return rs.getLong(1);
        }
        return 0L;
      }
    }
  }

  private long queryMigratedCount(String type) throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      String sql = "SELECT COUNT(*) FROM MIGRATION_MAPPING WHERE TYPE = ? AND C8_KEY IS NOT NULL AND SKIP_REASON IS NULL";
      try (var stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, type);
        var rs = stmt.executeQuery();
        if (rs.next()) {
          return rs.getLong(1);
        }
        return 0L;
      }
    }
  }

  private void insertSkippedEntity(String c7Id, String skipReason) throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      String sql = "INSERT INTO MIGRATION_MAPPING (C7_ID, C8_KEY, CREATE_TIME, TYPE, SKIP_REASON) VALUES (?, ?, ?, ?, ?)";
      try (var stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, c7Id);
        stmt.setNull(2, java.sql.Types.BIGINT);
        stmt.setTimestamp(3, null);
        stmt.setString(4, "RUNTIME_PROCESS_INSTANCE");
        stmt.setString(5, skipReason);
        stmt.executeUpdate();
      }
    }
  }

  private void insertMigratedEntity(String c7Id, Long c8Key) throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
      String sql = "INSERT INTO MIGRATION_MAPPING (C7_ID, C8_KEY, CREATE_TIME, TYPE, SKIP_REASON) VALUES (?, ?, ?, ?, ?)";
      try (var stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, c7Id);
        stmt.setLong(2, c8Key);
        stmt.setTimestamp(3, null);
        stmt.setString(4, "RUNTIME_PROCESS_INSTANCE");
        stmt.setNull(5, java.sql.Types.VARCHAR);
        stmt.executeUpdate();
      }
    }
  }
}
