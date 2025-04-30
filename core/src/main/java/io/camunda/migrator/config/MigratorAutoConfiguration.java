/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config;

import io.camunda.migrator.HistoryMigrator;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.converter.ConverterConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Import({
    SpringProcessEngineServicesConfiguration.class,
    JacksonConfiguration.class,
    MyBatisConfiguration.class,
    RdbmsConfiguration.class,
    ConverterConfiguration.class,
    HistoryMigrator.class,
    RuntimeMigrator.class,
})
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@Configuration
public class MigratorAutoConfiguration {

  @Configuration
  static class DataSourcesConfiguration {

    @Bean
    @ConfigurationProperties("migrator.source")
    public DataSource sourceDataSource() {
      return DataSourceBuilder.create().build();
    }

    @Bean
    public PlatformTransactionManager sourceTransactionManager(@Qualifier("sourceDataSource") DataSource sourceDataSource) {
      return new DataSourceTransactionManager(sourceDataSource);
    }

    @Bean
    @ConfigurationProperties("migrator.target")
    public DataSource targetDataSource() {
      return DataSourceBuilder.create().build();
    }

    @Bean
    public PlatformTransactionManager targetTransactionManager(@Qualifier("targetDataSource") DataSource targetDataSource) {
      return new DataSourceTransactionManager(targetDataSource);
    }
  }

  @Autowired
  protected DataSource sourceDataSource;

  @Autowired
  protected PlatformTransactionManager sourceTransactionManager;

  @Bean
  public ProcessEngineConfigurationImpl processEngineConfigurationImpl() {
    var config = new SpringProcessEngineConfiguration();
    config.setDataSource(sourceDataSource);
    config.setTransactionManager(sourceTransactionManager);
    config.setHistory("auto");
    config.setJobExecutorActivate(false);
    config.setDatabaseSchemaUpdate("create-drop");
    return config;
  }

  @Configuration
  static class PecConfiguration {

    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfigurationImpl;

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean() {
      final ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
      factoryBean.setProcessEngineConfiguration(processEngineConfigurationImpl);

      return factoryBean;
    }
  }

}
