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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MigratorApp {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  protected static final int MAX_ARGUMENTS = 3;

  protected static final String ARG_HELP = "help";
  protected static final String ARG_HISTORY_MIGRATION = "history";
  protected static final String ARG_RUNTIME_MIGRATION = "runtime";
  protected static final String ARG_RETRY_SKIPPED = "retry-skipped";
  protected static final String ARG_LIST_SKIPPED = "list-skipped";

  protected static final Set<String> VALID_FLAGS = Set.of(
      "--" + ARG_RUNTIME_MIGRATION,
      "--" + ARG_HISTORY_MIGRATION,
      "--" + ARG_LIST_SKIPPED,
      "--" + ARG_RETRY_SKIPPED,
      "--" + ARG_HELP
  );

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
    ConfigurableApplicationContext context = SpringApplication.run(MigratorApp.class, args);
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
        migrateHistory(context, mode);
      } else if (appArgs.containsOption(ARG_RUNTIME_MIGRATION)) {
        migrateRuntime(context, mode);
      } else if (appArgs.containsOption(ARG_HISTORY_MIGRATION)) {
        migrateHistory(context, mode);
      } else {
        LOGGER.warn("Invalid argument combination");
      }
    } finally {
      SpringApplication.exit(context);
    }
  }

  protected static void validateArguments(String[] args) {
    if (args.length > MAX_ARGUMENTS) {
      throw new IllegalArgumentException("Too many arguments.");
    }

    for (String arg : args) {
      if (!VALID_FLAGS.contains(arg)) {
        throw new IllegalArgumentException("Invalid flag: " + arg);
      }
    }
  }

  protected static void printUsage() {
    System.out.println();
    System.out.println("Usage: start.sh [--help] [--runtime] [--history] [--list-skipped|--retry-skipped]");
    System.out.println("Options:");
    System.out.println("  --help            - Show this help message");
    System.out.println("  --runtime         - Migrate runtime data only");
    System.out.println("  --history         - Migrate history data only. This option is still EXPERIMENTAL and not meant for production use.");
    System.out.println("  --list-skipped    - List previously skipped data");
    System.out.println("  --retry-skipped   - Retry only previously skipped data");
  }

  public static void migrateRuntime(ConfigurableApplicationContext context, MigratorMode mode) {
    LOGGER.info("Migrating runtime data...");
    RuntimeMigrator runtimeMigrator = context.getBean(RuntimeMigrator.class);
    runtimeMigrator.setMode(mode);
    runtimeMigrator.start();
  }

  public static void migrateHistory(ConfigurableApplicationContext context, MigratorMode mode) {
    LOGGER.info("Migrating history data...");
    HistoryMigrator historyMigrator = context.getBean(HistoryMigrator.class);
    historyMigrator.setMode(mode);
    historyMigrator.migrate();
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
