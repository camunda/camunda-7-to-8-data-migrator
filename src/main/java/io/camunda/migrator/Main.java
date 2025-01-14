package io.camunda.migrator;

import io.camunda.migrator.service.C7MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    var context = SpringApplication.run(Main.class, args);

    LOGGER.info("C7 Data Migrator Application Started");

    try {
      C7MigrationService migrationService = context.getBean(C7MigrationService.class);
      migrationService.execute();

      LOGGER.info("C7 Data Migrator Application Ended");
    } catch (Exception e) {
      LOGGER.error("Camunda Migrator finished with error", e);
    } finally {
      SpringApplication.exit(context);
    }

  }

}
