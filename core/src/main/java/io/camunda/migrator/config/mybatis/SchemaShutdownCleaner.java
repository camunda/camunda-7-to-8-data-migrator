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
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = MigratorProperties.PREFIX, name = "auto-drop", havingValue = "true")
public class SchemaShutdownCleaner {

  public static final String DB_CHANGELOG = "db/changelog/migrator/db.changelog-master.yaml";

  @Autowired
  @Qualifier("migratorDataSource")
  protected DataSource dataSource;

  @Autowired
  protected MigratorProperties configProperties;

  @PreDestroy
  public void cleanUp() {
    if(configProperties.getAutoDrop()) {
      callApi(this::dropAll, FAILED_TO_DROP_MIGRATION_TABLE);
    }
  }

  public void dropAll() {
    try (Connection conn = this.dataSource.getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
      Liquibase liquibase = new Liquibase(DB_CHANGELOG, new ClassLoaderResourceAccessor(), database);
      liquibase.dropAll(); // drop objects created by liquibase
      liquibase.clearCheckSums(); // clear checksums to allow re-running the changelog
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }
}