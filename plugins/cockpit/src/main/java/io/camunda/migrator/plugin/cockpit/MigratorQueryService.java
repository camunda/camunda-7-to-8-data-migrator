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
import java.util.function.BiFunction;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.webapp.impl.db.QueryServiceImpl;

public class MigratorQueryService<T> extends QueryServiceImpl implements Command<T> {

  protected Map<String, Object> params;
  protected BiFunction<Map<String, Object>, CommandContext, T> queryExecutor;

  public MigratorQueryService(Map<String, Object> params,
                              BiFunction<Map<String, Object>, CommandContext, T> queryExecutor) {
    super(null);
    this.params = params;
    this.queryExecutor = queryExecutor;
  }

  @Override
  public T execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl engineConfig = getProcessEngineConfiguration(commandContext);
    configureAuthCheck(new ListQueryParameterObject(), engineConfig, commandContext);

    Properties props = new Properties();
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(String.format("db/properties/%s.properties", getDatabaseType(engineConfig)))) {
      if (is != null) {
        props.load(is);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    params.putAll((Map<String, Object>) (Map<?, ?>) props);
    return queryExecutor.apply(params, commandContext);
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
