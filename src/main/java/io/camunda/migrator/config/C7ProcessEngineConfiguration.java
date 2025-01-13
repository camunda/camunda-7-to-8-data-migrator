package io.camunda.migrator.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class C7ProcessEngineConfiguration {

  @Bean
  public ProcessEngine processEngine(@Value("${migrator.camunda7.engine.jdbc-url}") String jdbcUrl,
                                      @Value("${migrator.camunda7.engine.jdbc-username}") String jdbcUsername,
                                      @Value("${migrator.camunda7.engine.jdbc-password}") String jdbcPassword,
                                      @Value("${migrator.camunda7.engine.jdbc-driver}") String jdbcDriver) {
    return ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
        .setJdbcUrl(jdbcUrl)
        .setJdbcUsername(jdbcUsername)
        .setJdbcPassword(jdbcPassword)
        .setJdbcDriver(jdbcDriver)
        .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
        .buildProcessEngine();
  }
}
