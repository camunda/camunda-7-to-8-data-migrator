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

    // Retrieve the bean from the context
    C7MigrationService myService = context.getBean(C7MigrationService.class);
    myService.migrate();

    LOGGER.info("C7 Data Migrator Application Ended");
    SpringApplication.exit(context);
  }

}
