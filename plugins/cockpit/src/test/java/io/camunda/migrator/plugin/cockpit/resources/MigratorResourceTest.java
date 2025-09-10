/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.plugin.cockpit.resources;

import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Comparator;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.camunda.bpm.cockpit.Cockpit;
import org.camunda.bpm.cockpit.db.CommandExecutor;
import org.camunda.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.Before;
import org.junit.Test;

public class MigratorResourceTest extends AbstractCockpitPluginTest {

  private MigratorResource resource;

  @Before
  public void setUp() throws Exception {
    super.before();
    ProcessEngine processEngine = getProcessEngine();
    resource = new MigratorResource(processEngine.getName());
    runLiquibaseMigrations();
  }

  @Test
  public void testMigratorResource() {
    // given - insert multiple migrated records
    IdKeyDbModel expectedMigrated1 = createExpectedMigratedModel("migratedLegacyId1");
    IdKeyDbModel expectedMigrated2 = createExpectedMigratedModel("migratedLegacyId2");
    insertTestData(expectedMigrated1);
    insertTestData(expectedMigrated2);
    String processInstanceType = String.valueOf(IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);

    // when - query initial state
    Long initialMigratedCount = resource.getMigratedCount(processInstanceType);
    List<IdKeyDbModel> initialMigratedInstances = resource.getMigrated(processInstanceType, 0, 10);
    Long initialSkippedCount = resource.getSkippedCount(processInstanceType);
    List<IdKeyDbModel> initialSkippedInstances = resource.getSkipped(processInstanceType, 0, 10);

    // then - verify initial state
    assertThat(initialMigratedCount).isEqualTo(2L);
    assertThat(initialSkippedCount).isEqualTo(0L);
    assertThat(initialMigratedInstances.size()).isEqualTo(2);
    assertThat(initialSkippedInstances.size()).isEqualTo(0);
    assertIdKeyDbModelListsEqual(List.of(expectedMigrated1, expectedMigrated2), initialMigratedInstances);

    // given - insert multiple skipped records
    IdKeyDbModel expectedSkipped1 = createExpectedSkippedModel("SkippedLegacyId1", "Test skip reason 1");
    IdKeyDbModel expectedSkipped2 = createExpectedSkippedModel("SkippedLegacyId2", "Test skip reason 2");
    insertTestData(expectedSkipped1);
    insertTestData(expectedSkipped2);

    // when - query final state
    Long finalMigratedCount = resource.getMigratedCount(processInstanceType);
    List<IdKeyDbModel> finalMigratedInstances = resource.getMigrated(processInstanceType, 0, 10);
    Long finalSkippedCount = resource.getSkippedCount(processInstanceType);
    List<IdKeyDbModel> finalSkippedInstances = resource.getSkipped(processInstanceType, 0, 10);

    assertThat(finalMigratedCount).isEqualTo(2L); // should remain unchanged
    assertThat(finalSkippedCount).isEqualTo(2L);
    assertThat(finalMigratedInstances.size()).isEqualTo(2);
    assertThat(finalSkippedInstances.size()).isEqualTo(2);

    // then verify content
    assertIdKeyDbModelListsEqual(List.of(expectedMigrated1, expectedMigrated2), finalMigratedInstances);
    assertIdKeyDbModelListsEqual(List.of(expectedSkipped1, expectedSkipped2), finalSkippedInstances);

    // test pagination - offset 1, limit 1
    List<IdKeyDbModel> migratedSecondPage = resource.getMigrated(processInstanceType, 1, 1);
    List<IdKeyDbModel> skippedSecondPage = resource.getSkipped(processInstanceType, 1, 1);
    assertThat(migratedSecondPage.size()).isEqualTo(1);
    assertThat(skippedSecondPage.size()).isEqualTo(1);

    // test pagination - offset 10, limit 1 (beyond available data)
    List<IdKeyDbModel> migratedBeyondData = resource.getMigrated(processInstanceType, 10, 1);
    List<IdKeyDbModel> skippedBeyondData = resource.getSkipped(processInstanceType, 10, 1);
    assertThat(migratedBeyondData.size()).isEqualTo(0);
    assertThat(skippedBeyondData.size()).isEqualTo(0);
  }

  private void assertIdKeyDbModelListsEqual(List<IdKeyDbModel> expected, List<IdKeyDbModel> actual) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.size()).isEqualTo(expected.size());

    if (expected.isEmpty()) {
      return;
    }

    // Sort both lists by ID for comparison
    List<IdKeyDbModel> sortedExpected = expected.stream()
        .sorted(Comparator.comparing(IdKeyDbModel::id))
        .toList();
    List<IdKeyDbModel> sortedActual = actual.stream()
        .sorted(Comparator.comparing(IdKeyDbModel::id))
        .toList();

    // Compare each element
    for (int i = 0; i < sortedExpected.size(); i++) {
      IdKeyDbModel expectedModel = sortedExpected.get(i);
      IdKeyDbModel actualModel = sortedActual.get(i);

      assertThat(actualModel.id()).isEqualTo(expectedModel.id());
      assertThat(actualModel.instanceKey()).isEqualTo(expectedModel.instanceKey());
      assertThat(actualModel.type()).isEqualTo(expectedModel.type());
      assertThat(actualModel.skipReason()).isEqualTo(expectedModel.skipReason());
    }
  }

  private IdKeyDbModel createExpectedMigratedModel(String legacyId) {
    IdKeyDbModel model = new IdKeyDbModel();
    model.setId(legacyId);
    model.setInstanceKey(getNextKey());
    model.setType(IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);
    return model;
  }

  private IdKeyDbModel createExpectedSkippedModel(String legacyId, String skipReason) {
    IdKeyDbModel model = createExpectedMigratedModel(legacyId);
    model.setSkipReason(skipReason);
    model.setInstanceKey(null);
    return model;
  }

  private static void insertTestData(IdKeyDbModel idKeyDbModel) {
    try (Connection conn = DriverManager.getConnection(
        "jdbc:h2:mem:default-process-engine;DB_CLOSE_DELAY=-1", "sa", "")) {
      String insertSql = "INSERT INTO MIGRATION_MAPPING (ID, INSTANCE_KEY, START_DATE, TYPE, SKIP_REASON) VALUES (?, ?, ?, ?, ?)";
      try (var stmt = conn.prepareStatement(insertSql)) {
        stmt.setString(1, idKeyDbModel.id());
        stmt.setTimestamp(3, null);
        stmt.setString(4, String.valueOf(idKeyDbModel.type()));
        stmt.setString(5, idKeyDbModel.skipReason());
        if(idKeyDbModel.instanceKey() == null) {
          stmt.setNull(2, java.sql.Types.BIGINT);
        } else {
          stmt.setLong(2, idKeyDbModel.instanceKey());
        }
        stmt.executeUpdate();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to insert test data", e);
    }
  }

  private void runLiquibaseMigrations() throws Exception {
    try (Connection conn = DriverManager.getConnection(
        "jdbc:h2:mem:default-process-engine;DB_CLOSE_DELAY=-1", "sa", "")) {

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

  protected CommandExecutor getCommandExecutor() {
    return Cockpit.getCommandExecutor("default");
  }
}
