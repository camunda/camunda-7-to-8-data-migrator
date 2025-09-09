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
import io.camunda.migrator.impl.clients.DbClient;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.util.Optional;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = MigratorProperties.PREFIX, name = "drop-schema", havingValue = "true")
public class SchemaShutdownCleaner {

  protected static final Logger LOGGER = LoggerFactory.getLogger(SchemaShutdownCleaner.class);

  @Autowired
  @Qualifier("migratorDataSource")
  protected DataSource dataSource;

  @Autowired
  protected MigratorProperties configProperties;

  @Autowired
  protected DbClient dbClient;

  @PreDestroy
  public void cleanUp() {
    if (configProperties.getDropSchema()) {
      Long skipped = dbClient.countSkipped();
      if (skipped == 0) {
        String tablePrefix = Optional.ofNullable(configProperties.getTablePrefix()).orElse("");
        callApi(() -> rollbackTableCreation(tablePrefix), FAILED_TO_DROP_MIGRATION_TABLE);
      } else {
        LOGGER.warn(FAILED_TO_DROP_MIGRATION_TABLE + ": [{}] entities were skipped during migration.", skipped);
      }
    }
  }

  private void rollbackTableCreation(String prefix) {
    try (Connection conn = dataSource.getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
      Liquibase liquibase = new Liquibase("db/changelog/migrator/db.0.0.1.xml", new ClassLoaderResourceAccessor(), database);
      database.setDatabaseChangeLogTableName(prefix + "DATABASECHANGELOG");
      database.setDatabaseChangeLogLockTableName(prefix + "DATABASECHANGELOGLOCK");
      liquibase.setChangeLogParameter("prefix", prefix);
      liquibase.clearCheckSums();
      liquibase.rollback("tag_before_create_migration_mapping_table", "");
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

}