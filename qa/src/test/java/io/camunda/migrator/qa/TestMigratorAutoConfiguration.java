package io.camunda.migrator.qa;

import io.camunda.migrator.config.MigratorAutoConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestMigratorAutoConfiguration {

  @Autowired
  protected MigratorAutoConfiguration migratorAutoConfiguration;

  @Bean
  @Primary
  public ProcessEngineConfigurationImpl processEngineConfiguration() {
    return migratorAutoConfiguration.processEngineConfigurationImpl().setDatabaseSchemaUpdate("create-drop");
  }
}
