/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.MigratorMode.LIST_SKIPPED;
import static io.camunda.migrator.MigratorMode.MIGRATE;
import static io.camunda.migrator.MigratorMode.RETRY_SKIPPED;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENTS;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_FLOW_NODE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_INCIDENT;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_USER_TASK;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_VARIABLE;

import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessDefinitionDbQuery;
import io.camunda.db.rdbms.sql.DecisionDefinitionMapper;
import io.camunda.db.rdbms.sql.DecisionRequirementsMapper;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.DecisionRequirementsDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.config.C8DataSourceConfigured;
import io.camunda.migrator.converter.DecisionDefinitionConverter;
import io.camunda.migrator.converter.DecisionRequirementsDefinitionConverter;
import io.camunda.migrator.converter.FlowNodeConverter;
import io.camunda.migrator.converter.IncidentConverter;
import io.camunda.migrator.converter.ProcessDefinitionConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.migrator.converter.UserTaskConverter;
import io.camunda.migrator.converter.VariableConverter;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.logging.HistoryMigratorLogs;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.impl.util.ExceptionUtils;
import io.camunda.migrator.impl.util.PrintUtils;
import io.camunda.search.entities.ProcessDefinitionEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionRequirementsDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(C8DataSourceConfigured.class)
public class HistoryMigrator {

  public static final Set<IdKeyMapper.TYPE> HISTORY_TYPES = EnumSet.allOf(IdKeyMapper.TYPE.class)
      .stream()
      .filter(type -> type.name().startsWith("HISTORY"))
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(IdKeyMapper.TYPE.class)));

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

  @Autowired
  private DecisionRequirementsMapper decisionRequirementsMapper;

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

  @Autowired
  private DecisionRequirementsDefinitionConverter decisionRequirementsConverter;

  protected MigratorMode mode = MIGRATE;

  public void start() {
    try {
      ExceptionUtils.setContext(ExceptionUtils.ExceptionContext.HISTORY);
      if (LIST_SKIPPED.equals(mode)) {
        printSkippedHistoryEntities();
      } else {
        migrate();
      }
    } finally {
      ExceptionUtils.clearContext();
    }
  }

  private void printSkippedHistoryEntities() {
    HISTORY_TYPES.forEach(this::printSkippedEntitiesForType);
  }

  private void printSkippedEntitiesForType(IdKeyMapper.TYPE type) {
    PrintUtils.printSkippedInstancesHeader(dbClient.countSkippedByType(type), type);
    dbClient.listSkippedEntitiesByType(type);
  }

  public void migrate() {
    migrateProcessDefinitions();
    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();
    migrateDecisionRequirementsDefinitions();
    migrateDecisionDefinitions();
  }

  public void migrateProcessDefinitions() {
    HistoryMigratorLogs.migratingProcessDefinitions();
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_PROCESS_DEFINITION, idKeyDbModel -> {
        ProcessDefinition historicProcessDefinition = c7Client.getProcessDefinition(idKeyDbModel.id());
        migrateProcessDefinition(historicProcessDefinition);
      });
    } else {
      c7Client.fetchAndHandleProcessDefinitions(this::migrateProcessDefinition, dbClient.findLatestStartDateByType((HISTORY_PROCESS_DEFINITION)));
    }
  }

  private void migrateProcessDefinition(ProcessDefinition legacyProcessDefinition) {
    String legacyId = legacyProcessDefinition.getId();
    if (shouldMigrate(legacyId, HISTORY_PROCESS_DEFINITION)) {
      HistoryMigratorLogs.migratingProcessDefinition(legacyId);
      ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(legacyProcessDefinition);
      processDefinitionMapper.insert(dbModel);
      Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyProcessDefinition.getDeploymentId());
      saveRecord(legacyId, deploymentTime, dbModel.processDefinitionKey(), HISTORY_PROCESS_DEFINITION);
      HistoryMigratorLogs.migratingProcessDefinitionCompleted(legacyId);
    }
  }

  private void migrateProcessInstances() {
    HistoryMigratorLogs.migratingProcessInstances();
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_PROCESS_INSTANCE, idKeyDbModel -> {
        HistoricProcessInstance historicProcessInstance = c7Client.getHistoricProcessInstance(idKeyDbModel.id());
        migrateProcessInstance(historicProcessInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricProcessInstances(this::migrateProcessInstance, dbClient.findLatestStartDateByType((HISTORY_PROCESS_INSTANCE)));
    }
  }

  private void migrateProcessInstance(HistoricProcessInstance legacyProcessInstance) {
    String legacyProcessInstanceId = legacyProcessInstance.getId();
    if (shouldMigrate(legacyProcessInstanceId, HISTORY_PROCESS_INSTANCE)) {
      HistoryMigratorLogs.migratingProcessInstance(legacyProcessInstanceId);
      Long processDefinitionKey = findProcessDefinitionKey(legacyProcessInstance.getProcessDefinitionId());
      String processDefinitionId = legacyProcessInstance.getProcessDefinitionId();

      if(isMigrated(processDefinitionId, HISTORY_PROCESS_DEFINITION)) {
        String legacySuperProcessInstanceId = legacyProcessInstance.getSuperProcessInstanceId();
        Long parentProcessInstanceKey = null;
        if (legacySuperProcessInstanceId != null) {
          ProcessInstanceEntity parentInstance = findProcessInstanceByLegacyId(legacySuperProcessInstanceId);
          if (parentInstance != null) {
            parentProcessInstanceKey = parentInstance.processInstanceKey();
          }
        }
        if (parentProcessInstanceKey != null || legacySuperProcessInstanceId == null) {
          ProcessInstanceDbModel dbModel = processInstanceConverter.apply(legacyProcessInstance, processDefinitionKey, parentProcessInstanceKey);
          processInstanceMapper.insert(dbModel);
          saveRecord(legacyProcessInstanceId, legacyProcessInstance.getStartTime(), dbModel.processInstanceKey(), HISTORY_PROCESS_INSTANCE);
          HistoryMigratorLogs.migratingProcessInstanceCompleted(legacyProcessInstanceId);
        } else {
          saveRecord(legacyProcessInstanceId, null, HISTORY_PROCESS_INSTANCE);
          HistoryMigratorLogs.skippingProcessInstanceDueToMissingParent(legacyProcessInstanceId);
        }
      } else {
        saveRecord(legacyProcessInstanceId, null, HISTORY_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingProcessInstanceDueToMissingDefinition(legacyProcessInstanceId);
      }
    }
  }

  public void migrateDecisionRequirementsDefinitions() {
    HistoryMigratorLogs.migratingDecisionRequirements();

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_DECISION_REQUIREMENTS, idKeyDbModel -> {
        DecisionRequirementsDefinition legacyDecisionRequirement = c7Client.getDecisionRequirementsDefinition(
            idKeyDbModel.id());
        migrateDecisionRequirementsDefinition(legacyDecisionRequirement);
      });
    } else {
      c7Client.fetchAndHandleDecisionRequirementsDefinitions(this::migrateDecisionRequirementsDefinition);
    }
  }

  private void migrateDecisionRequirementsDefinition(DecisionRequirementsDefinition legacyDecisionRequirements) {
    String legacyId = legacyDecisionRequirements.getId();
    if (shouldMigrate(legacyId, HISTORY_DECISION_REQUIREMENTS)) {
      HistoryMigratorLogs.migratingDecisionRequirements(legacyId);
      DecisionRequirementsDbModel dbModel = decisionRequirementsConverter.apply(legacyDecisionRequirements);
      decisionRequirementsMapper.insert(dbModel);
      saveRecord(legacyId, dbModel.decisionRequirementsKey(), HISTORY_DECISION_REQUIREMENTS);
      HistoryMigratorLogs.migratingDecisionRequirementsCompleted(legacyId);
    }
  }

  public void migrateDecisionDefinitions() {
    HistoryMigratorLogs.migratingDecisionDefinitions();

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_DECISION_DEFINITION, idKeyDbModel -> {
        DecisionDefinition legacyDecisionDefinition = c7Client.getDecisionDefinition(idKeyDbModel.id());
        migrateDecisionDefinition(legacyDecisionDefinition);
      });
    } else {
      c7Client.fetchAndHandleDecisionDefinitions(this::migrateDecisionDefinition,
          dbClient.findLatestStartDateByType((HISTORY_DECISION_DEFINITION)));
    }
  }

  private void migrateDecisionDefinition(DecisionDefinition legacyDecisionDefinition) {
    String legacyId = legacyDecisionDefinition.getId();
    if (shouldMigrate(legacyId, HISTORY_DECISION_DEFINITION)) {
      HistoryMigratorLogs.migratingDecisionDefinition(legacyId);
      Long decisionRequirementsKey = null;

      if (legacyDecisionDefinition.getDecisionRequirementsDefinitionId() != null) {
        decisionRequirementsKey = dbClient.findKeyByIdAndType(legacyDecisionDefinition.getDecisionRequirementsDefinitionId(), HISTORY_DECISION_REQUIREMENTS);

        if (decisionRequirementsKey == null) {
          saveRecord(legacyId, null, HISTORY_DECISION_DEFINITION);
          HistoryMigratorLogs.skippingDecisionDefinition(legacyId);
          return;
        }
      }

      DecisionDefinitionDbModel dbModel = decisionDefinitionConverter.apply(legacyDecisionDefinition,
          decisionRequirementsKey);
      decisionDefinitionMapper.insert(dbModel);
      Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyDecisionDefinition.getDeploymentId());
      saveRecord(legacyId, deploymentTime, dbModel.decisionDefinitionKey(), HISTORY_DECISION_DEFINITION);
      HistoryMigratorLogs.migratingDecisionDefinitionCompleted(legacyId);
    }
  }

  public void migrateIncidents() {
    HistoryMigratorLogs.migratingHistoricIncidents();
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_INCIDENT, idKeyDbModel -> {
        HistoricIncident historicIncident = c7Client.getHistoricIncident(idKeyDbModel.id());
        migrateIncident(historicIncident);
      });
    } else {
      c7Client.fetchAndHandleHistoricIncidents(this::migrateIncident, dbClient.findLatestStartDateByType((HISTORY_INCIDENT)));
    }
  }

  private void migrateIncident(HistoricIncident legacyIncident) {
    String legacyIncidentId = legacyIncident.getId();
    if (shouldMigrate(legacyIncidentId, HISTORY_INCIDENT)) {
      HistoryMigratorLogs.migratingHistoricIncident(legacyIncidentId);
      ProcessInstanceEntity legacyProcessInstance = findProcessInstanceByLegacyId(legacyIncident.getProcessInstanceId());
      if (legacyProcessInstance != null) {
        Long processInstanceKey = legacyProcessInstance.processInstanceKey();
        if (processInstanceKey != null) {
          Long flowNodeInstanceKey = findFlowNodeKey(legacyIncident.getActivityId(), legacyIncident.getProcessInstanceId());
          Long processDefinitionKey = findProcessDefinitionKey(legacyIncident.getProcessDefinitionId());
          Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
          IncidentDbModel dbModel = incidentConverter.apply(legacyIncident, processDefinitionKey, processInstanceKey, jobDefinitionKey, flowNodeInstanceKey);
          incidentMapper.insert(dbModel);
          saveRecord(legacyIncidentId, legacyIncident.getCreateTime(), dbModel.incidentKey(), HISTORY_INCIDENT);
          HistoryMigratorLogs.migratingHistoricIncidentCompleted(legacyIncidentId);
        } else {
          saveRecord(legacyIncidentId, null, HISTORY_INCIDENT);
          HistoryMigratorLogs.skippingHistoricIncident(legacyIncidentId);
        }
      } else {
        saveRecord(legacyIncidentId, null, HISTORY_INCIDENT);
        HistoryMigratorLogs.skippingHistoricIncident(legacyIncidentId);
      }
    }
  }

  public void migrateVariables() {
    HistoryMigratorLogs.migratingHistoricVariables();

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_VARIABLE, idKeyDbModel -> {
        HistoricVariableInstance historicVariableInstance = c7Client.getHistoricVariableInstance(idKeyDbModel.id());
        migrateVariable(historicVariableInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricVariables(this::migrateVariable, dbClient.findLatestIdByType(HISTORY_VARIABLE));
    }
  }

  private void migrateVariable(HistoricVariableInstance legacyVariable) {
    String legacyVariableId = legacyVariable.getId();
    if (shouldMigrate(legacyVariableId, HISTORY_VARIABLE)) {
      HistoryMigratorLogs.migratingHistoricVariable(legacyVariableId);

      String taskId = legacyVariable.getTaskId();
      if (taskId != null && !isMigrated(taskId, HISTORY_USER_TASK)) {
        // Skip variable if it belongs to a skipped task
        saveRecord(legacyVariableId, null, IdKeyMapper.TYPE.HISTORY_VARIABLE);
        HistoryMigratorLogs.skippingHistoricVariableDueToMissingTask(legacyVariableId, taskId);
        return;
      }

      String legacyProcessInstanceId = legacyVariable.getProcessInstanceId();
      if (isMigrated(legacyProcessInstanceId, HISTORY_PROCESS_INSTANCE)) {
        if (isMigrated(legacyVariable.getActivityInstanceId(), HISTORY_FLOW_NODE)) {
          ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyProcessInstanceId);
          Long processInstanceKey = processInstance.processInstanceKey();
          Long scopeKey = findFlowNodeKey(legacyVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
          if (scopeKey != null) {
            VariableDbModel dbModel = variableConverter.apply(legacyVariable, processInstanceKey, scopeKey);
            variableMapper.insert(dbModel);
            saveRecord(legacyVariableId, legacyVariable.getCreateTime(), dbModel.variableKey(), HISTORY_VARIABLE);
            HistoryMigratorLogs.migratingHistoricVariableCompleted(legacyVariableId);
          } else {
            saveRecord(legacyVariableId, null, IdKeyMapper.TYPE.HISTORY_VARIABLE);
            HistoryMigratorLogs.skippingHistoricVariableDueToMissingScopeKey(legacyVariableId);
          }
        } else {
          saveRecord(legacyVariableId, null, IdKeyMapper.TYPE.HISTORY_VARIABLE);
          HistoryMigratorLogs.skippingHistoricVariableDueToMissingFlowNode(legacyVariableId);
        }
      } else {
        saveRecord(legacyVariableId, null, IdKeyMapper.TYPE.HISTORY_VARIABLE);
        HistoryMigratorLogs.skippingHistoricVariableDueToMissingProcessInstance(legacyVariableId);
      }
    }
  }

  public void migrateUserTasks() {
    HistoryMigratorLogs.migratingHistoricUserTasks();

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_USER_TASK, idKeyDbModel -> {
        HistoricTaskInstance historicTaskInstance = c7Client.getHistoricTaskInstance(idKeyDbModel.id());
        migrateUserTask(historicTaskInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricUserTasks(this::migrateUserTask, dbClient.findLatestStartDateByType((HISTORY_USER_TASK)));
    }
  }

  private void migrateUserTask(HistoricTaskInstance legacyUserTask) {
    String legacyUserTaskId = legacyUserTask.getId();
    if (shouldMigrate(legacyUserTaskId, HISTORY_USER_TASK)) {
      HistoryMigratorLogs.migratingHistoricUserTask(legacyUserTaskId);
      if (isMigrated(legacyUserTask.getProcessInstanceId(), HISTORY_PROCESS_INSTANCE)) {
        ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyUserTask.getProcessInstanceId());
        if (isMigrated(legacyUserTask.getActivityInstanceId(), HISTORY_FLOW_NODE)) {
          Long elementInstanceKey = findFlowNodeKey(legacyUserTask.getActivityInstanceId());
          Long processDefinitionKey = findProcessDefinitionKey(legacyUserTask.getProcessDefinitionId());
          UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstance, elementInstanceKey);
          userTaskMapper.insert(dbModel);
          saveRecord(legacyUserTaskId, legacyUserTask.getStartTime(), dbModel.userTaskKey(), HISTORY_USER_TASK);
          HistoryMigratorLogs.migratingHistoricUserTaskCompleted(legacyUserTaskId);
        } else {
          saveRecord(legacyUserTaskId, null, IdKeyMapper.TYPE.HISTORY_USER_TASK);
          HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingFlowNode(legacyUserTaskId);
        }
      } else {
        saveRecord(legacyUserTaskId, null, IdKeyMapper.TYPE.HISTORY_USER_TASK);
        HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingProcessInstance(legacyUserTaskId);
      }
    }
  }

  public void migrateFlowNodes() {
    HistoryMigratorLogs.migratingHistoricFlowNodes();

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_FLOW_NODE, idKeyDbModel -> {
        HistoricActivityInstance historicActivityInstance = c7Client.getHistoricActivityInstance(idKeyDbModel.id());
        migrateFlowNode(historicActivityInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricFlowNodes(this::migrateFlowNode, dbClient.findLatestStartDateByType((HISTORY_FLOW_NODE)));
    }
  }

  private void migrateFlowNode(HistoricActivityInstance legacyFlowNode) {
    String legacyFlowNodeId = legacyFlowNode.getId();
    if (shouldMigrate(legacyFlowNodeId, HISTORY_FLOW_NODE)) {
      HistoryMigratorLogs.migratingHistoricFlowNode(legacyFlowNodeId);
      ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyFlowNode.getProcessInstanceId());
      if (processInstance != null) {
        Long processInstanceKey = processInstance.processInstanceKey();
        Long processDefinitionKey = findProcessDefinitionKey(legacyFlowNode.getProcessDefinitionId());
        FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(legacyFlowNode, processDefinitionKey, processInstanceKey);
        flowNodeMapper.insert(dbModel);
        saveRecord(legacyFlowNodeId, legacyFlowNode.getStartTime(), dbModel.flowNodeInstanceKey(), HISTORY_FLOW_NODE);
        HistoryMigratorLogs.migratingHistoricFlowNodeCompleted(legacyFlowNodeId);
      } else {
        saveRecord(legacyFlowNodeId, null, HISTORY_FLOW_NODE);
        HistoryMigratorLogs.skippingHistoricFlowNode(legacyFlowNodeId);
      }
    }
  }

  protected ProcessInstanceEntity findProcessInstanceByLegacyId(String processInstanceId) {
    if (processInstanceId == null)
      return null;

    Long key = dbClient.findKeyByIdAndType(processInstanceId, HISTORY_PROCESS_INSTANCE);
    if (key == null) {
      return null;
    }

    return processInstanceMapper.findOne(key);
  }

  private Long findProcessDefinitionKey(String processDefinitionId) {
    Long key = dbClient.findKeyByIdAndType(processDefinitionId, HISTORY_PROCESS_DEFINITION);
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
    Long key = dbClient.findKeyByIdAndType(processInstanceId, HISTORY_PROCESS_INSTANCE);
    if (key == null) {
      return null;
    }

    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(FlowNodeInstanceDbQuery.of(
        b -> b.filter(FlowNodeInstanceFilter.of(f -> f.flowNodeIds(activityId).flowNodeInstanceKeys(key)))));

    if (!flowNodes.isEmpty()) {
      return flowNodes.getFirst().flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityInstanceId) {
    Long key = dbClient.findKeyByIdAndType(activityInstanceId, HISTORY_FLOW_NODE);
    if (key == null) {
      return null;
    }

    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b -> b.filter(f -> f.flowNodeInstanceKeys(key))));

    if (!flowNodes.isEmpty()) {
      return flowNodes.getFirst().flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  private boolean isMigrated(String id, IdKeyMapper.TYPE type) {
    return dbClient.checkHasKeyByIdAndType(id, type);
  }

  private boolean shouldMigrate(String id, IdKeyMapper.TYPE type) {
     if (mode == RETRY_SKIPPED) {
      return !dbClient.checkHasKeyByIdAndType(id, type);
    }
    return !dbClient.checkExistsByIdAndType(id, type);
  }

  protected void saveRecord(String entityId, Long entityKey, IdKeyMapper.TYPE type) {
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.updateKeyByIdAndType(entityId, entityKey, type);
    } else if (MIGRATE.equals(mode)) {
      dbClient.insert(entityId, entityKey, type);
    }
  }

  protected void saveRecord(String entityId, Date date, Long entityKey, IdKeyMapper.TYPE type) {
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.updateKeyByIdAndType(entityId, entityKey, type);
    } else if (MIGRATE.equals(mode)) {
      dbClient.insert(entityId, date, entityKey, type);
    }
  }

  public void setMode(MigratorMode mode) {
    this.mode = mode;
  }

}
