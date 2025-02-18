package io.camunda.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    var context = SpringApplication.run(Main.class, args);

    LOGGER.info("C7 Data Migrator Application Started");

    try {
      CamundaMigrator migrationService = context.getBean(CamundaMigrator.class);
      migrationService.migrateAllHistoricProcessInstances();

      LOGGER.info("C7 Data Migrator Application Ended");
    } catch (Exception e) {
      LOGGER.error("Camunda Migrator finished with error", e);
    } finally {
      SpringApplication.exit(context);
    }

  }

}
