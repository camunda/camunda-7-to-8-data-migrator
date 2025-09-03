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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = MigratorProperties.PREFIX, name = "auto-drop", havingValue = "true")
public class SchemaShutdownCleaner {

  private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS %s";

  @Autowired
  @Qualifier("migratorDataSource")
  protected DataSource dataSource;

  @Autowired
  protected MigratorProperties configProperties;

  @PreDestroy
  public void cleanUp() {
    if (configProperties.getAutoDrop()) {
      callApi(this::dropTableIfExists, FAILED_TO_DROP_MIGRATION_TABLE);
    }
  }

  private void dropTableIfExists() {
    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(String.format(DROP_TABLE_SQL, getPrefix() + "MIGRATION_MAPPING"));
    } catch (SQLException e) {
      throw new PersistenceException(e);
    }
  }

  private String getPrefix() {
    return Optional.ofNullable(configProperties.getTablePrefix()).orElse("");
  }
}