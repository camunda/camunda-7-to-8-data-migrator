/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_FLOW_NODE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_INCIDENT;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_USER_TASK;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_VARIABLE;

import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessDefinitionDbQuery;
import io.camunda.db.rdbms.sql.DecisionDefinitionMapper;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.config.C8DataSourceConfigured;
import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.converter.DecisionDefinitionConverter;
import io.camunda.migrator.converter.FlowNodeConverter;
import io.camunda.migrator.converter.IncidentConverter;
import io.camunda.migrator.converter.ProcessDefinitionConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.migrator.converter.UserTaskConverter;
import io.camunda.migrator.converter.VariableConverter;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.logging.HistoryMigratorLogs;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessDefinitionEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(C8DataSourceConfigured.class)
public class HistoryMigrator {

  // Mappers

  @Autowired
  private ProcessInstanceMapper processInstanceMapper;

  @Autowired
  private UserTaskMapper userTaskMapper;

  @Autowired
  private VariableMapper variableMapper;

  @Autowired
  private IncidentMapper incidentMapper;

  @Autowired
  private ProcessDefinitionMapper processDefinitionMapper;

  @Autowired
  private DecisionDefinitionMapper decisionDefinitionMapper;

  @Autowired
  private FlowNodeInstanceMapper flowNodeMapper;

  // Services

  @Autowired
  protected MigratorProperties migratorProperties;

  // Clients

  @Autowired
  protected DbClient dbClient;

  @Autowired
  protected C7Client c7Client;

  // Converters

  @Autowired
  private ProcessInstanceConverter processInstanceConverter;

  @Autowired
  private FlowNodeConverter flowNodeConverter;

  @Autowired
  private UserTaskConverter userTaskConverter;

  @Autowired
  private VariableConverter variableConverter;

  @Autowired
  private IncidentConverter incidentConverter;

  @Autowired
  private ProcessDefinitionConverter processDefinitionConverter;

  @Autowired
  private DecisionDefinitionConverter decisionDefinitionConverter;

  public void migrate() {
    migrateProcessDefinitions();
    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();

    //migrateDecisionDefinitions();
  }

  private void migrateDecisionDefinitions() {
    HistoryMigratorLogs.migratingDecisionDefinitions();

    Consumer<DecisionDefinition> callback = legacyDecisionDefinition -> {
      String legacyId = legacyDecisionDefinition.getId();
      HistoryMigratorLogs.migratingDecisionDefinition(legacyId);
      DecisionDefinitionDbModel dbModel = decisionDefinitionConverter.apply(legacyDecisionDefinition);
      decisionDefinitionMapper.insert(dbModel);
      Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyDecisionDefinition.getDeploymentId());
      dbClient.insert(legacyId, deploymentTime, dbModel.decisionDefinitionKey(), HISTORY_DECISION_DEFINITION);
      HistoryMigratorLogs.migratingDecisionDefinitionCompleted(legacyId);
    };
    c7Client.fetchAndProcessDecisionDefinitions(callback,
        dbClient.findLatestStartDateByType((HISTORY_DECISION_DEFINITION)));
  }

  private void migrateProcessDefinitions() {
    HistoryMigratorLogs.migratingProcessDefinitions();
    Consumer<ProcessDefinition> callback = legacyProcessDefinition -> {
      String legacyId = legacyProcessDefinition.getId();
      if (!dbClient.checkExists(legacyId)) {
        HistoryMigratorLogs.migratingProcessDefinition(legacyId);
        ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(legacyProcessDefinition);
        processDefinitionMapper.insert(dbModel);
        Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyProcessDefinition.getDeploymentId());
        dbClient.insert(legacyId, deploymentTime, dbModel.processDefinitionKey(), HISTORY_PROCESS_DEFINITION);
        HistoryMigratorLogs.migratingProcessDefinitionCompleted(legacyId);
      }
    };

    c7Client.fetchAndProcessProcessDefinitions(callback,
        dbClient.findLatestStartDateByType((HISTORY_PROCESS_DEFINITION)));
  }

  private void migrateProcessInstances() {
    HistoryMigratorLogs.migratingProcessInstances();
    Consumer<HistoricProcessInstance> callback = legacyProcessInstance -> {
      String legacyProcessInstanceId = legacyProcessInstance.getId();
      if (!dbClient.checkExists(legacyProcessInstanceId)) {
        HistoryMigratorLogs.migratingProcessInstance(legacyProcessInstanceId);
        Long processDefinitionKey = findProcessDefinitionKey(legacyProcessInstance.getProcessDefinitionId());
        if (processDefinitionKey != null) {
          String legacySuperProcessInstanceId = legacyProcessInstance.getSuperProcessInstanceId();
          Long parentProcessInstanceKey = null;
          if (legacySuperProcessInstanceId != null) {
            ProcessInstanceEntity parentInstance = findProcessInstanceByLegacyId(legacySuperProcessInstanceId);
            if (parentInstance != null) {
              parentProcessInstanceKey = parentInstance.processInstanceKey();
            }
          }
          if (parentProcessInstanceKey != null || legacySuperProcessInstanceId == null) {
            // Continue if PI has no parent
            ProcessInstanceDbModel dbModel = processInstanceConverter.apply(legacyProcessInstance, processDefinitionKey,
                parentProcessInstanceKey);
            processInstanceMapper.insert(dbModel);
            dbClient.insert(legacyProcessInstanceId, legacyProcessInstance.getStartTime(), dbModel.processInstanceKey(),
                HISTORY_PROCESS_INSTANCE);
            HistoryMigratorLogs.migratingProcessInstanceCompleted(legacyProcessInstanceId);
          } else {
            HistoryMigratorLogs.skippingProcessInstanceDueToMissingParent(legacyProcessInstanceId);
          }
        } else {
          HistoryMigratorLogs.skippingProcessInstanceDueToMissingDefinition(legacyProcessInstanceId);
        }
      }
    };

    c7Client.fetchAndProcessHistoricProcessInstances(callback,
        dbClient.findLatestStartDateByType((HISTORY_PROCESS_INSTANCE)));
  }

  private void migrateIncidents() {
    HistoryMigratorLogs.migratingHistoricIncidents();
    Consumer<HistoricIncident> callback = legacyIncident -> {
      String legacyIncidentId = legacyIncident.getId();
      if (!dbClient.checkExists(legacyIncidentId)) {
        HistoryMigratorLogs.migratingHistoricIncident(legacyIncidentId);
        ProcessInstanceEntity legacyProcessInstance = findProcessInstanceByLegacyId(
            legacyIncident.getProcessInstanceId());
        if (legacyProcessInstance != null) {
          Long processInstanceKey = legacyProcessInstance.processInstanceKey();
          if (processInstanceKey != null) {
            Long flowNodeInstanceKey = findFlowNodeKey(legacyIncident.getActivityId(),
                legacyIncident.getProcessInstanceId());
            Long processDefinitionKey = findProcessDefinitionKey(legacyIncident.getProcessDefinitionId());
            Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
            IncidentDbModel dbModel = incidentConverter.apply(legacyIncident, processDefinitionKey, processInstanceKey,
                jobDefinitionKey, flowNodeInstanceKey);
            incidentMapper.insert(dbModel);
            dbClient.insert(legacyIncidentId, legacyIncident.getCreateTime(), dbModel.incidentKey(), HISTORY_INCIDENT);
            HistoryMigratorLogs.migratingHistoricIncidentCompleted(legacyIncidentId);
          } else {
            HistoryMigratorLogs.skippingHistoricIncident(legacyIncidentId);
          }
        }
      }
    };

    c7Client.fetchAndProcessHistoricIncidents(callback, dbClient.findLatestStartDateByType((HISTORY_INCIDENT)));
  }

  private void migrateVariables() {
    HistoryMigratorLogs.migratingHistoricVariables();
    Consumer<HistoricVariableInstance> callback = legacyVariable -> {
      String legacyVariableId = legacyVariable.getId();
      if (!dbClient.checkExists(legacyVariableId)) {
        HistoryMigratorLogs.migratingHistoricVariable(legacyVariableId);
        String legacyProcessInstanceId = legacyVariable.getProcessInstanceId();
        ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyProcessInstanceId);

        if (processInstance != null) {
          Long processInstanceKey = processInstance.processInstanceKey();
          Long scopeKey = findFlowNodeKey(
              legacyVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
          if (scopeKey != null) {
            VariableDbModel dbModel = variableConverter.apply(legacyVariable, processInstanceKey, scopeKey);
            variableMapper.insert(dbModel);
            dbClient.insert(legacyVariableId, legacyVariable.getCreateTime(), dbModel.variableKey(), HISTORY_VARIABLE);
            HistoryMigratorLogs.migratingHistoricVariableCompleted(legacyVariableId);
          } else {
            HistoryMigratorLogs.skippingHistoricVariableDueToMissingFlowNode(legacyVariableId);
          }
        } else {
          HistoryMigratorLogs.skippingHistoricVariableDueToMissingProcessInstance(legacyVariableId);
        }
      }
    };

    c7Client.fetchAndProcessHistoricVariables(callback, dbClient.findLatestIdByType(HISTORY_VARIABLE));
  }

  private void migrateUserTasks() {
    HistoryMigratorLogs.migratingHistoricUserTasks();
    Consumer<HistoricTaskInstance> callback = legacyUserTask -> {
      String legacyUserTaskId = legacyUserTask.getId();
      if (!dbClient.checkExists(legacyUserTaskId)) {
        HistoryMigratorLogs.migratingHistoricUserTask(legacyUserTaskId);
        ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyUserTask.getProcessInstanceId());
        if (processInstance != null) {
          Long elementInstanceKey = findFlowNodeKey(legacyUserTask.getActivityInstanceId());
          if (elementInstanceKey != null) {
            Long processDefinitionKey = findProcessDefinitionKey(legacyUserTask.getProcessDefinitionId());
            UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstance,
                elementInstanceKey);
            userTaskMapper.insert(dbModel);
            dbClient.insert(legacyUserTaskId, legacyUserTask.getStartTime(), dbModel.userTaskKey(), HISTORY_USER_TASK);
            HistoryMigratorLogs.migratingHistoricUserTaskCompleted(legacyUserTaskId);
          } else {
            HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingFlowNode(legacyUserTaskId);
          }
        } else {
          HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingProcessInstance(legacyUserTaskId);
        }
      }
    };

    c7Client.fetchAndProcessHistoricUserTasks(callback, dbClient.findLatestStartDateByType((HISTORY_USER_TASK)));
  }

  private void migrateFlowNodes() {
    HistoryMigratorLogs.migratingHistoricFlowNodes();
    Consumer<HistoricActivityInstance> callback = legacyFlowNode -> {
      String legacyFlowNodeId = legacyFlowNode.getId();
      if (!dbClient.checkExists(legacyFlowNodeId)) {
        HistoryMigratorLogs.migratingHistoricFlowNode(legacyFlowNodeId);
        ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyFlowNode.getProcessInstanceId());
        if (processInstance != null) {
          Long processInstanceKey = processInstance.processInstanceKey();
          Long processDefinitionKey = findProcessDefinitionKey(legacyFlowNode.getProcessDefinitionId());
          FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(legacyFlowNode, processDefinitionKey,
              processInstanceKey);
          flowNodeMapper.insert(dbModel);
          dbClient.insert(legacyFlowNodeId, legacyFlowNode.getStartTime(), dbModel.flowNodeInstanceKey(),
              HISTORY_FLOW_NODE);
          HistoryMigratorLogs.migratingHistoricFlowNodeCompleted(legacyFlowNodeId);
        } else {
          HistoryMigratorLogs.skippingHistoricFlowNode(legacyFlowNodeId);
        }
      }
    };
    c7Client.fetchAndProcessHistoricFlowNodes(callback, dbClient.findLatestStartDateByType((HISTORY_FLOW_NODE)));
  }

  protected ProcessInstanceEntity findProcessInstanceByLegacyId(String processInstanceId) {
    if (processInstanceId == null)
      return null;

    Long key = dbClient.findKeyById(processInstanceId);
    if (key == null) {
      return null;
    }

    return processInstanceMapper.findOne(key);
  }

  private Long findProcessDefinitionKey(String processDefinitionId) {
    Long key = dbClient.findKeyById(processDefinitionId);
    if (key == null) {
      return null;
    }

    List<ProcessDefinitionEntity> processDefinitions = processDefinitionMapper.search(
        ProcessDefinitionDbQuery.of(b -> b.filter(value -> value.processDefinitionKeys(key))));

    if (!processDefinitions.isEmpty()) {
      return processDefinitions.getFirst().processDefinitionKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityId, String processInstanceId) {
    Long key = dbClient.findKeyById(processInstanceId);
    if (key == null) {
      return null;
    }

    List<FlowNodeInstanceEntity> flowNodes = flowNodeMapper.search(FlowNodeInstanceDbQuery.of(
        b -> b.filter(FlowNodeInstanceFilter.of(f -> f.flowNodeIds(activityId).flowNodeInstanceKeys(key)))));

    if (!flowNodes.isEmpty()) {
      return flowNodes.getFirst().flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityInstanceId) {
    Long key = dbClient.findKeyById(activityInstanceId);
    if (key == null) {
      return null;
    }

    List<FlowNodeInstanceEntity> flowNodes = flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b -> b.filter(f -> f.flowNodeInstanceKeys(key))));

    if (!flowNodes.isEmpty()) {
      return flowNodes.getFirst().flowNodeInstanceKey();
    } else {
      return null;
    }
  }
}
