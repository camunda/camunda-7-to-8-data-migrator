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
package org.camunda.bpm.cockpit.plugin.sample.resources;

import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.util.HashMap;
import java.util.List;
import jakarta.ws.rs.GET;

import org.camunda.bpm.cockpit.plugin.resource.AbstractCockpitPluginResource;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.webapp.db.CommandExecutor;
import org.camunda.bpm.webapp.impl.db.QueryServiceImpl;

/**
 *
 * @author nico.rehwaldt
 */
public class ProcessInstanceResource extends AbstractCockpitPluginResource {

  public ProcessInstanceResource(String engineName) {
    super(engineName);
  }

  @GET
  public List<IdKeyDbModel> getProcessInstanceCounts() {
    return getCommandExecutor().executeCommand(new ListCommand(getCommandExecutor()));
  }

  protected class ListCommand extends QueryServiceImpl implements Command<List<IdKeyDbModel>> {

    public ListCommand(CommandExecutor commandExecutor) {
      super(commandExecutor);
    }

    public List<IdKeyDbModel> execute(CommandContext commandContext) {
      ProcessEngineConfigurationImpl engineConfig = getProcessEngineConfiguration(commandContext);
      configureAuthCheck(new ListQueryParameterObject(), engineConfig, commandContext);

      var parameters = new HashMap<>();
      parameters.put("type", IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);
      parameters.put("offset", 0);
      parameters.put("limit", 10);

      return (List<IdKeyDbModel>) commandContext.getDbSqlSession()
          .selectList("io.camunda.migrator.impl.persistence.IdKeyMapper.findSkippedByType", parameters);
    }
  }

}
