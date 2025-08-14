/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.migrator.impl.clients.C7Client;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

import static io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel.ProcessInstanceDbModelBuilder;
import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;
import static io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState;

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
        .elementId(null) // TODO: activityId in C7 but not part of the historic process instance. Not yet populated by RDBMS.
        .parentElementInstanceKey(null) // TODO: Call activity instance id that created the process. Not part of C7 historic process instance.
        // create historyService.createHistoricActivityInstanceQuery().calledProcessInstanceId(processInstance.getId()).singleResult().getId() in C7
        // then we map it to the C8 key in migration mapping

        // the element key of the call activity in C8? should we skip the migration of PI if we don't have the call activity yet?
        .numIncidents(getIncidents(processInstance)) // TODO: test
//        .partitionId(0) // TODO can we used C7_HISTORY_PARTITION_ID?
//        .treePath(null) // TODO io.camunda.exporter.rdbms.handlers.ProcessInstanceExportHandler.createTreePath // TODO what does it do?
        .historyCleanupDate(convertDate(processInstance.getRemovalTime())) // TODO: change if the removal time is always populated
//        .versionTag(processInstance.getProcessDefinitionVersionTag()) // TODO
        .build();
  }

  private int getIncidents(HistoricProcessInstance processInstance) {
    return Math.toIntExact(c7Client.getIncidentsByProcessInstance(processInstance.getId()));
  }

  private ProcessInstanceState convertState(String state) {
    return switch (state) {
      case "ACTIVE", "SUSPENDED" -> ProcessInstanceState.ACTIVE;
      case "COMPLETED" -> ProcessInstanceState.COMPLETED;
      case "EXTERNALLY_TERMINATED", "INTERNALLY_TERMINATED" -> ProcessInstanceState.CANCELED;

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }
}
