/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import static io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel.ProcessInstanceDbModelBuilder;
import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.util.ConverterUtil;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessInstanceConverter {

  @Autowired
  protected C7Client c7Client;

  public ProcessInstanceDbModel apply(HistoricProcessInstance processInstance,
                                      Long processDefinitionKey,
                                      Long parentProcessInstanceKey) {
    return new ProcessInstanceDbModelBuilder()
        .processInstanceKey(getNextKey())
        // Get key from runtime instance/model migration
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(processInstance.getProcessDefinitionKey())
        .startDate(convertDate(processInstance.getStartTime()))
        .endDate(convertDate(processInstance.getEndTime()))
        .state(convertState(processInstance.getState()))
        .tenantId(processInstance.getTenantId())
        .version(processInstance.getProcessDefinitionVersion())
        // parent and super process instance are used synonym (process instance that contained the call activity)
        .parentProcessInstanceKey(parentProcessInstanceKey)
        // TODO: Call activity instance id that created the process in C8. No yet migrated from C7.
        // https://github.com/camunda/camunda-bpm-platform/issues/5359
        //        .parentElementInstanceKey(null)
        //        .treePath(null)
        .numIncidents(getIncidents(processInstance))
        .partitionId(ConverterUtil.C7_HISTORY_PARTITION_ID)
        .historyCleanupDate(convertDate(processInstance.getRemovalTime()))
        .build();
  }

  protected ProcessInstanceState convertState(String state) {
    return switch (state) {
      case "ACTIVE", "SUSPENDED" -> ProcessInstanceState.ACTIVE;
      case "COMPLETED" -> ProcessInstanceState.COMPLETED;
      case "EXTERNALLY_TERMINATED", "INTERNALLY_TERMINATED" -> ProcessInstanceState.CANCELED;

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }

  protected int getIncidents(HistoricProcessInstance processInstance) {
    return Math.toIntExact(c7Client.getIncidentsByProcessInstance(processInstance.getId()));
  }

}
