package io.camunda.migrator.example;

import io.camunda.migrator.RuntimeMigrator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RuntimeExampleApplication {

  public static void main(String[] args) {
    var context = SpringApplication.run(RuntimeExampleApplication.class, args);

    try {
      RuntimeMigrator runtimeMigrator = context.getBean(RuntimeMigrator.class);
      runtimeMigrator.migrate();
    } finally {
      SpringApplication.exit(context);
    }

  }

}
