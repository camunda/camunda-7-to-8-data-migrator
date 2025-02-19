/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
@Configuration
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
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
