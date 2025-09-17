/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.app;


import io.camunda.migrator.impl.AutoDeployer;
import io.camunda.migrator.HistoryMigrator;
import io.camunda.migrator.MigratorMode;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MigratorApp {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  protected static final int MAX_ARGUMENTS = 5;

  protected static final String ARG_HELP = "help";
  protected static final String ARG_HISTORY_MIGRATION = "history";
  protected static final String ARG_RUNTIME_MIGRATION = "runtime";
  protected static final String ARG_RETRY_SKIPPED = "retry-skipped";
  protected static final String ARG_LIST_SKIPPED = "list-skipped";
  protected static final String ARG_DROP_SCHEMA = "drop-schema";
  protected static final String ARG_FORCE = "force";

  protected static final Set<String> VALID_FLAGS = Set.of(
      "--" + ARG_RUNTIME_MIGRATION,
      "--" + ARG_HISTORY_MIGRATION,
      "--" + ARG_LIST_SKIPPED,
      "--" + ARG_RETRY_SKIPPED,
      "--" + ARG_DROP_SCHEMA,
      "--" + ARG_FORCE,
      "--" + ARG_HELP
  );

  protected static final Set<String> VALID_ENTITY_TYPES = IdKeyMapper.getHistoryTypeNames();

  public static void main(String[] args) {
    try {
      // Early validation before Spring Boot starts
      validateArguments(args);
    } catch (IllegalArgumentException e) {
      LOGGER.error("Error: {}", e.getMessage());
      printUsage();
      System.exit(1);
    }

    if (args.length == 0) {
      LOGGER.info("Starting application without migration flags");
    } else {
      LOGGER.info("Starting migration with flags: {}", String.join(" ", args));
    }

    // Continue with Spring Boot application
    ConfigurableApplicationContext context = new SpringApplicationBuilder(MigratorApp.class).run(args);
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);
    MigratorMode mode = getMigratorMode(appArgs);
    try {
      AutoDeployer autoDeployer = context.getBean(AutoDeployer.class);
      autoDeployer.deploy();
      if (appArgs.containsOption(ARG_HELP)) {
        printUsage();
        System.exit(1);
      } else if (shouldRunFullMigration(appArgs)) {
        LOGGER.info("Migrating both runtime and history");
        migrateRuntime(context, mode);
        migrateHistory(context, appArgs, mode);
      } else if (appArgs.containsOption(ARG_RUNTIME_MIGRATION)) {
        migrateRuntime(context, mode);
      } else if (appArgs.containsOption(ARG_HISTORY_MIGRATION)) {
        migrateHistory(context, appArgs, mode);
      } else {
        LOGGER.warn("Invalid argument combination");
      }
    } finally {
      SpringApplication.exit(context);
    }
  }

  protected static void validateArguments(String[] args) {
    List<String> argsList = java.util.Arrays.asList(args);
    boolean listSkippedHistoryFound = argsList.contains("--" + ARG_LIST_SKIPPED) &&
                                      argsList.contains("--" + ARG_HISTORY_MIGRATION);
    int flagCount = 0;

    for (String arg : args) {
      if (VALID_FLAGS.contains(arg)) {
        flagCount++;
      } else if (listSkippedHistoryFound && VALID_ENTITY_TYPES.contains(arg)) {
        // Valid entity type parameter following --list-skipped with --history
        continue;
      } else {
        throw new IllegalArgumentException("Invalid flag: " + arg);
      }
    }

    // Check if we have too many flags (not counting entity type parameters)
    if (flagCount > MAX_ARGUMENTS) {
      throw new IllegalArgumentException("Error: Too many arguments.");
    }
  }

  protected static void printUsage() {
    System.out.println();
    System.out.println("Usage: start.sh/bat [--help] [--runtime] [--history] [--list-skipped [ENTITY_TYPES...]|--retry-skipped] [--drop-schema]");
    System.out.println("Options:");
    System.out.println("  --help            - Show this help message");
    System.out.println("  --runtime         - Migrate runtime data only");
    System.out.println("  --history         - Migrate history data only. This option is still EXPERIMENTAL and not meant for production use.");
    System.out.println("  --list-skipped [ENTITY_TYPES...]");
    System.out.println("                    - List previously skipped migration data. For history data, optionally specify entity types to filter.");
    System.out.println("                      Filter only applicable with history migration. Available entity types:");
    System.out.println("                      HISTORY_PROCESS_DEFINITION, HISTORY_PROCESS_INSTANCE, HISTORY_INCIDENT,");
    System.out.println("                      HISTORY_VARIABLE, HISTORY_USER_TASK, HISTORY_FLOW_NODE,");
    System.out.println("                      HISTORY_DECISION_INSTANCE, HISTORY_DECISION_DEFINITION");
    System.out.println("  --retry-skipped   - Retry only previously skipped history data");
    System.out.println("  --drop-schema     - If migration was successful, drop the migrator schema on shutdown");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  start.sh --history --list-skipped");
    System.out.println("  start.sh --history --list-skipped HISTORY_PROCESS_INSTANCE HISTORY_USER_TASK");
  }

  public static void migrateRuntime(ConfigurableApplicationContext context, MigratorMode mode) {
    LOGGER.info("Migrating runtime data...");
    RuntimeMigrator runtimeMigrator = context.getBean(RuntimeMigrator.class);
    runtimeMigrator.setMode(mode);
    runtimeMigrator.start();
  }

  public static void migrateHistory(ConfigurableApplicationContext context, ApplicationArguments appArgs, MigratorMode mode) {
    LOGGER.info("Migrating history data...");
    HistoryMigrator historyMigrator = context.getBean(HistoryMigrator.class);
    historyMigrator.setMode(mode);

    // Extract entity type filters if --list-skipped is used
    if (mode == MigratorMode.LIST_SKIPPED) {
      List<IdKeyMapper.TYPE> entityTypeFilters = extractEntityTypeFilters(appArgs);
      if (!entityTypeFilters.isEmpty()) {
        historyMigrator.setRequestedEntityTypes(entityTypeFilters);
      }
    }

    historyMigrator.start();
  }

  protected static List<IdKeyMapper.TYPE> extractEntityTypeFilters(ApplicationArguments appArgs) {
    List<String> nonOptionArgs = appArgs.getNonOptionArgs();
    return nonOptionArgs.stream()
        .filter(VALID_ENTITY_TYPES::contains)
        .map(IdKeyMapper.TYPE::valueOf)
        .toList();
  }

  protected static boolean shouldRunFullMigration(ApplicationArguments appArgs) {
    return appArgs.containsOption(ARG_RUNTIME_MIGRATION) && appArgs.containsOption(ARG_HISTORY_MIGRATION);
  }

  protected static MigratorMode getMigratorMode(ApplicationArguments appArgs) {
    if (appArgs.containsOption(ARG_LIST_SKIPPED)) {
      return MigratorMode.LIST_SKIPPED;
    } else if (appArgs.containsOption(ARG_RETRY_SKIPPED)) {
      return MigratorMode.RETRY_SKIPPED;
    } else {
      return MigratorMode.MIGRATE;
    }
  }
}
