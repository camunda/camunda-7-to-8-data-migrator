/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.bpm.cockpit.plugin.sample.resources;

import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.webapp.db.CommandExecutor;
import org.camunda.bpm.webapp.impl.db.QueryServiceImpl;

class ListCommand extends QueryServiceImpl implements Command<List<IdKeyDbModel>> {

  protected Map<String, Object> parameters;

  public ListCommand(CommandExecutor commandExecutor, Map<String, Object> parameters) {
    super(commandExecutor);
    this.parameters = parameters;
  }

  @SuppressWarnings("unchecked")
  public List<IdKeyDbModel> execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl engineConfig = getProcessEngineConfiguration(commandContext);
    configureAuthCheck(new ListQueryParameterObject(), engineConfig, commandContext);
    return (List<IdKeyDbModel>) commandContext.getDbSqlSession()
        .selectList("io.camunda.migrator.impl.persistence.IdKeyMapper.findSkippedByType", parameters);
  }

}
