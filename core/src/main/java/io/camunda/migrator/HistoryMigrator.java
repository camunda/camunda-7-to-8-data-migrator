/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.MigratorMode.LIST_SKIPPED;
import static io.camunda.migrator.MigratorMode.MIGRATE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.getHistoryTypes;

import io.camunda.migrator.config.C8DataSourceConfigured;
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.util.ExceptionUtils;
import io.camunda.migrator.impl.util.PrintUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(C8DataSourceConfigured.class)
public class HistoryMigrator {

  @Autowired
  private AsyncHistoryMigrator asyncHistoryMigrator;

  // Clients

  @Autowired
  protected DbClient dbClient;

  protected MigratorMode mode = MIGRATE;

  private List<TYPE> requestedEntityTypes;

  public void start() {
    try {
      ExceptionUtils.setContext(ExceptionUtils.ExceptionContext.HISTORY);
      if (LIST_SKIPPED.equals(mode)) {
        printSkippedHistoryEntities();
      } else {
        migrate();
      }
    } finally {
      ExceptionUtils.clearContext();
    }
  }

  private void printSkippedHistoryEntities() {
    if(requestedEntityTypes == null ||  requestedEntityTypes.isEmpty()) {
      getHistoryTypes().forEach(this::printSkippedEntitiesForType);
    } else {
      requestedEntityTypes.forEach(this::printSkippedEntitiesForType);
    }
  }

  private void printSkippedEntitiesForType(TYPE type) {
    PrintUtils.printSkippedInstancesHeader(dbClient.countSkippedByType(type), type);
    dbClient.listSkippedEntitiesByType(type);
  }

  public void migrate() {
    asyncHistoryMigrator.migrateProcessDefinitions()                             // Migrate process definitions asynchronously
        .thenCompose(v -> asyncHistoryMigrator.migrateProcessInstances())   // Trigger migration for process instances but only after definitions are done
        .join();                                            // Wait for instances to be completed

    var incidentsFuture = asyncHistoryMigrator.migrateIncidents();               // Migrate incidents asynchronously

    var flowNodesFuture = asyncHistoryMigrator.migrateFlowNodes()                // Migrate flow nodes asynchronously
        .thenCompose(v -> asyncHistoryMigrator.migrateUserTasks())         // After flow nodes are done, migrate user tasks asynchronously
        .thenCompose(v -> asyncHistoryMigrator.migrateVariables());        // After user tasks are done, migrate variables asynchronously

    var decisionsFuture = asyncHistoryMigrator.migrateDecisionRequirementsDefinitions() // Migrate decision requirements definitions asynchronously
        .thenCompose(v -> asyncHistoryMigrator.migrateDecisionDefinitions())       // After requirements are done, migrate decision definitions
        .thenCompose(v -> asyncHistoryMigrator.migrateDecisionInstances());        // After decision definitions are done, migrate decision instances asynchronously

    CompletableFuture.allOf(incidentsFuture, flowNodesFuture, decisionsFuture).join(); // Wait for all futures to be completed before exiting
    // Finished migration
  }

  public void setMode(MigratorMode mode) {
    this.mode = mode;
    asyncHistoryMigrator.setMode(mode);
  }

  public void setRequestedEntityTypes(List<TYPE> requestedEntityTypes) {
    this.requestedEntityTypes = requestedEntityTypes;
  }

}
