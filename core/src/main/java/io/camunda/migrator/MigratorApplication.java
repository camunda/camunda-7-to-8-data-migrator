package io.camunda.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
public class MigratorApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorApplication.class);

  public static void main(String[] args) {
    var context = SpringApplication.run(MigratorApplication.class, args);

    LOGGER.info("C7 Data Migrator Application Started");

    try {
      RuntimeMigrator runtimeMigrator = context.getBean(RuntimeMigrator.class);
      runtimeMigrator.migrate();

      HistoryMigrator historyMigrator = context.getBean(HistoryMigrator.class);
      historyMigrator.migrate();

      LOGGER.info("C7 Data Migrator Application Ended");
    } catch (Exception e) {
      LOGGER.error("Camunda Migrator finished with error", e);
    } finally {
      SpringApplication.exit(context);
    }

  }

}
