package io.camunda.migrator.qa;

import io.camunda.migrator.RuntimeMigrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RuntimeExampleApplication {

  protected static ConfigurableApplicationContext context;

  @Autowired
  protected RuntimeMigrator runtimeMigrator;

  public static void main(String[] args) {
    context = SpringApplication.run(RuntimeExampleApplication.class, args);
  }

}
