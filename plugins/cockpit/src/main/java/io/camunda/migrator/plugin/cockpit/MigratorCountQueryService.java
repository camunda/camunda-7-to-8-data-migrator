/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.plugin.cockpit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.webapp.impl.db.QueryServiceImpl;

public class MigratorCountQueryService extends QueryServiceImpl implements Command<Long> {

  protected Map<String, Object> parameters;

  public MigratorCountQueryService(Map<String, Object> parameters) {
    super(null);
    this.parameters = parameters;
  }

  @Override
  public Long execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl engineConfig = getProcessEngineConfiguration(commandContext);
    configureAuthCheck(new ListQueryParameterObject(), engineConfig, commandContext);

    Properties props = new Properties();
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(String.format("db/properties/%s.properties", getDatabaseType(engineConfig)))) {
      if (is != null) {
        props.load(is);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    parameters.putAll((Map<String, Object>) (Map<?, ?>) props);
    return (Long) commandContext.getDbSqlSession()
        .selectOne("io.camunda.migrator.impl.persistence.IdKeyMapper.countSkippedByType", parameters);
  }

  protected Object getDatabaseType(ProcessEngineConfigurationImpl engineConfig) {
    String databaseType = engineConfig.getDatabaseType();
    if ("postgres".equals(databaseType)) {
      return "postgresql";
    } else {
      return databaseType;
    }
  }
}
