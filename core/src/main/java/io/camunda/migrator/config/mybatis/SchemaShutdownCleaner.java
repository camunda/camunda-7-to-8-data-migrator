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
import java.util.Optional;
import javax.sql.DataSource;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = MigratorProperties.PREFIX, name = "auto-drop", havingValue = "true")
public class SchemaShutdownCleaner {

  @Autowired
  @Qualifier("migratorDataSource")
  protected DataSource dataSource;

  @Autowired
  protected MigratorProperties configProperties;

  @Autowired
  protected MigratorConfiguration migratorConfiguration;

  @PreDestroy
  public void cleanUp() {
    if (configProperties.getAutoDrop()) {
      String tablePrefix = Optional.ofNullable(configProperties.getTablePrefix()).orElse("");
      callApi(() -> dropTableIfExists(tablePrefix), FAILED_TO_DROP_MIGRATION_TABLE);
    }
  }

  public void dropTableIfExists(String tablePrefix) {
    try {
      MultiTenantSpringLiquibase dropLiquibase = migratorConfiguration.createSchema(
          dataSource, tablePrefix, "db/changelog/migrator/db.changelog-drop.yaml"
      );
      dropLiquibase.afterPropertiesSet(); // will execute the drop
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }
}