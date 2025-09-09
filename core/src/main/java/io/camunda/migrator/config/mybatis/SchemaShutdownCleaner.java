/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config.mybatis;

import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_DROP_MIGRATION_TABLE;
import static io.camunda.migrator.impl.util.ExceptionUtils.callApi;

import io.camunda.migrator.config.property.MigratorProperties;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = MigratorProperties.PREFIX, name = "drop-schema", havingValue = "true")
public class SchemaShutdownCleaner {

  public static final String DB_DROP_CHANGELOG = "db/changelog/migrator/db.changelog-drop.yaml";

  @Autowired
  @Qualifier("migratorDataSource")
  protected DataSource dataSource;

  @Autowired
  protected MigratorProperties configProperties;

  @Autowired
  protected MigratorConfiguration migratorConfiguration;

  @PreDestroy
  public void cleanUp() {
    if (configProperties.getDropSchema()) {
      String tablePrefix = Optional.ofNullable(configProperties.getTablePrefix()).orElse("");
      callApi(() -> executeDrop(tablePrefix), FAILED_TO_DROP_MIGRATION_TABLE);
    }
  }

  public void executeDrop(String tablePrefix) {
    try {
      var dropLiquibase = migratorConfiguration.createSchema(dataSource, tablePrefix, DB_DROP_CHANGELOG);
      dropLiquibase.afterPropertiesSet();
      deleteChangelogEntry(tablePrefix);
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  /**
   * Deletes the changelog entry from the database so that the schema can be recreated on next run
   */
  private void deleteChangelogEntry(String tablePrefix) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tablePrefix + "DATABASECHANGELOG WHERE ID=? AND AUTHOR=?")) {
      ps.setString(1, "create_migration_mapping_table");
      ps.setString(2, "Camunda");
      ps.executeUpdate();
    }
  }
}