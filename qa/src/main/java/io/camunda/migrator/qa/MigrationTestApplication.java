package io.camunda.migrator.qa;

import io.camunda.migrator.RuntimeMigrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MigrationTestApplication {

  @Autowired
  protected RuntimeMigrator runtimeMigrator;

  public static void main(String[] args) {
    SpringApplication.run(MigrationTestApplication.class, args);
  }

}
