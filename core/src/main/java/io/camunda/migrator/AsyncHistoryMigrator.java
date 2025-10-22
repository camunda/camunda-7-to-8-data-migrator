/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static io.camunda.migrator.MigratorMode.MIGRATE;
import static io.camunda.migrator.MigratorMode.RETRY_SKIPPED;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_INSTANCE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_DECISION_REQUIREMENT;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_FLOW_NODE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_INCIDENT;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_USER_TASK;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE.HISTORY_VARIABLE;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_BELONGS_TO_SKIPPED_TASK;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_FLOW_NODE;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_PARENT_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_PROCESS_DEFINITION;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_PROCESS_INSTANCE_KEY;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_SCOPE_KEY;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_DECISION_REQUIREMENTS;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_DECISION_DEFINITION;
import static io.camunda.migrator.impl.logging.HistoryMigratorLogs.SKIP_REASON_MISSING_PARENT_DECISION_INSTANCE;

import io.camunda.db.rdbms.read.domain.DecisionDefinitionDbQuery;
import io.camunda.db.rdbms.read.domain.DecisionInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessDefinitionDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessInstanceDbQuery;
import io.camunda.db.rdbms.sql.DecisionDefinitionMapper;
import io.camunda.db.rdbms.sql.DecisionInstanceMapper;
import io.camunda.db.rdbms.sql.DecisionRequirementsMapper;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.db.rdbms.write.domain.DecisionRequirementsDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.converter.DecisionDefinitionConverter;
import io.camunda.migrator.converter.DecisionInstanceConverter;
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
import io.camunda.search.entities.DecisionDefinitionEntity;
import io.camunda.search.entities.DecisionInstanceEntity;
import io.camunda.search.entities.ProcessDefinitionEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricDecisionInstance;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionRequirementsDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(HistoryMigrator.class)
public class AsyncHistoryMigrator {

  // Mappers

  @Autowired
  private ProcessInstanceMapper processInstanceMapper;

  @Autowired
  private DecisionInstanceMapper decisionInstanceMapper;

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
  private DecisionInstanceConverter decisionInstanceConverter;

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

  public void migrate() {
    migrateProcessDefinitions()                             // Migrate process definitions asynchronously
        .thenCompose(v -> migrateProcessInstances())   // Trigger migration for process instances but only after definitions are done
        .join();                                            // Wait for instances to be completed

    var incidentsFuture = migrateIncidents();               // Migrate incidents asynchronously

    var flowNodesFuture = migrateFlowNodes()                // Migrate flow nodes asynchronously
        .thenCompose(v -> migrateUserTasks())         // After flow nodes are done, migrate user tasks asynchronously
        .thenCompose(v -> migrateVariables());        // After user tasks are done, migrate variables asynchronously

    var decisionsFuture = migrateDecisionRequirementsDefinitions() // Migrate decision requirements definitions asynchronously
        .thenCompose(v -> migrateDecisionDefinitions())       // After requirements are done, migrate decision definitions
        .thenCompose(v -> migrateDecisionInstances());        // After decision definitions are done, migrate decision instances asynchronously

    CompletableFuture.allOf(incidentsFuture, flowNodesFuture, decisionsFuture).join(); // Wait for all futures to be completed before exiting
    // Finished migration
  }

  @Async
  public CompletableFuture<Void> migrateProcessDefinitions() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_PROCESS_DEFINITION);
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_PROCESS_DEFINITION, idKeyDbModel -> {
        ProcessDefinition historicProcessDefinition = c7Client.getProcessDefinition(idKeyDbModel.getC7Id());
        migrateProcessDefinition(historicProcessDefinition);
      });
    } else {
      c7Client.fetchAndHandleProcessDefinitions(this::migrateProcessDefinition, dbClient.findLatestCreateTimeByType((HISTORY_PROCESS_DEFINITION)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_PROCESS_DEFINITION);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateProcessDefinition(ProcessDefinition c7ProcessDefinition) {
    String c7Id = c7ProcessDefinition.getId();
    if (shouldMigrate(c7Id, HISTORY_PROCESS_DEFINITION)) {
      HistoryMigratorLogs.migratingProcessDefinition(c7Id);
      ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(c7ProcessDefinition);
      processDefinitionMapper.insert(dbModel);
      Date deploymentTime = c7Client.getDefinitionDeploymentTime(c7ProcessDefinition.getDeploymentId());
      markMigrated(c7Id, dbModel.processDefinitionKey(), deploymentTime, HISTORY_PROCESS_DEFINITION);
      HistoryMigratorLogs.migratingProcessDefinitionCompleted(c7Id);
    }
  }

  @Async
  public CompletableFuture<Void> migrateProcessInstances() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_PROCESS_INSTANCE);
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_PROCESS_INSTANCE, idKeyDbModel -> {
        HistoricProcessInstance historicProcessInstance = c7Client.getHistoricProcessInstance(idKeyDbModel.getC7Id());
        migrateProcessInstance(historicProcessInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricProcessInstances(this::migrateProcessInstance, dbClient.findLatestCreateTimeByType((HISTORY_PROCESS_INSTANCE)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_PROCESS_INSTANCE);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateProcessInstance(HistoricProcessInstance c7ProcessInstance) {
    String c7ProcessInstanceId = c7ProcessInstance.getId();
    if (shouldMigrate(c7ProcessInstanceId, HISTORY_PROCESS_INSTANCE)) {
      HistoryMigratorLogs.migratingProcessInstance(c7ProcessInstanceId);
      Long processDefinitionKey = findProcessDefinitionKey(c7ProcessInstance.getProcessDefinitionId());
      String processDefinitionId = c7ProcessInstance.getProcessDefinitionId();

      if (isMigrated(processDefinitionId, HISTORY_PROCESS_DEFINITION)) {
        String c7SuperProcessInstanceId = c7ProcessInstance.getSuperProcessInstanceId();
        Long parentProcessInstanceKey = null;
        if (c7SuperProcessInstanceId != null) {
          ProcessInstanceEntity parentInstance = findProcessInstanceByC7Id(c7SuperProcessInstanceId);
          if (parentInstance != null) {
            parentProcessInstanceKey = parentInstance.processInstanceKey();
          }
        }
        if (parentProcessInstanceKey != null || c7SuperProcessInstanceId == null) {
          ProcessInstanceDbModel dbModel = processInstanceConverter.apply(c7ProcessInstance, processDefinitionKey, parentProcessInstanceKey);
          processInstanceMapper.insert(dbModel);
          markMigrated(c7ProcessInstanceId, dbModel.processInstanceKey(), c7ProcessInstance.getStartTime(), HISTORY_PROCESS_INSTANCE);
          HistoryMigratorLogs.migratingProcessInstanceCompleted(c7ProcessInstanceId);
        } else {
          markSkipped(c7ProcessInstanceId, HISTORY_PROCESS_INSTANCE, c7ProcessInstance.getStartTime(), SKIP_REASON_MISSING_PARENT_PROCESS_INSTANCE);
          HistoryMigratorLogs.skippingProcessInstanceDueToMissingParent(c7ProcessInstanceId);
        }
      } else {
        markSkipped(c7ProcessInstanceId, HISTORY_PROCESS_INSTANCE, c7ProcessInstance.getStartTime(), SKIP_REASON_MISSING_PROCESS_DEFINITION);
        HistoryMigratorLogs.skippingProcessInstanceDueToMissingDefinition(c7ProcessInstanceId);
      }
    }
  }

  @Async
  public CompletableFuture<Void> migrateDecisionRequirementsDefinitions() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_DECISION_REQUIREMENT);


    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_DECISION_REQUIREMENT, idKeyDbModel -> {
        DecisionRequirementsDefinition c7DecisionRequirement = c7Client.getDecisionRequirementsDefinition(
            idKeyDbModel.getC7Id());
        migrateDecisionRequirementsDefinition(c7DecisionRequirement);
      });
    } else {
      c7Client.fetchAndHandleDecisionRequirementsDefinitions(this::migrateDecisionRequirementsDefinition);
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_DECISION_REQUIREMENT);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateDecisionRequirementsDefinition(DecisionRequirementsDefinition c7DecisionRequirements) {
    String c7Id = c7DecisionRequirements.getId();
    if (shouldMigrate(c7Id, HISTORY_DECISION_REQUIREMENT)) {
      HistoryMigratorLogs.migratingDecisionRequirements(c7Id);
      DecisionRequirementsDbModel dbModel = decisionRequirementsConverter.apply(c7DecisionRequirements);
      decisionRequirementsMapper.insert(dbModel);
      Date deploymentTime = c7Client.getDefinitionDeploymentTime(c7DecisionRequirements.getDeploymentId());
      markMigrated(c7Id, dbModel.decisionRequirementsKey(), deploymentTime, HISTORY_DECISION_REQUIREMENT);
      HistoryMigratorLogs.migratingDecisionRequirementsCompleted(c7Id);
    }
  }

  @Async
  public CompletableFuture<Void> migrateDecisionDefinitions() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_DECISION_DEFINITION);

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_DECISION_DEFINITION, idKeyDbModel -> {
        DecisionDefinition c7DecisionDefinition = c7Client.getDecisionDefinition(idKeyDbModel.getC7Id());
        migrateDecisionDefinition(c7DecisionDefinition);
      });
    } else {
      c7Client.fetchAndHandleDecisionDefinitions(this::migrateDecisionDefinition,
          dbClient.findLatestCreateTimeByType((HISTORY_DECISION_DEFINITION)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_DECISION_DEFINITION);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateDecisionDefinition(DecisionDefinition c7DecisionDefinition) {
    String c7Id = c7DecisionDefinition.getId();
    if (shouldMigrate(c7Id, HISTORY_DECISION_DEFINITION)) {
      HistoryMigratorLogs.migratingDecisionDefinition(c7Id);
      Long decisionRequirementsKey = null;

      Date deploymentTime = c7Client.getDefinitionDeploymentTime(c7DecisionDefinition.getDeploymentId());

      if (c7DecisionDefinition.getDecisionRequirementsDefinitionId() != null) {
        decisionRequirementsKey = dbClient.findC8KeyByC7IdAndType(c7DecisionDefinition.getDecisionRequirementsDefinitionId(),
            HISTORY_DECISION_REQUIREMENT);

        if (decisionRequirementsKey == null) {
          markSkipped(c7Id, HISTORY_DECISION_DEFINITION, deploymentTime, SKIP_REASON_MISSING_DECISION_REQUIREMENTS);
          HistoryMigratorLogs.skippingDecisionDefinition(c7Id);
          return;
        }
      }

      DecisionDefinitionDbModel dbModel = decisionDefinitionConverter.apply(c7DecisionDefinition, decisionRequirementsKey);
      decisionDefinitionMapper.insert(dbModel);
      markMigrated(c7Id, dbModel.decisionDefinitionKey(), deploymentTime, HISTORY_DECISION_DEFINITION);
      HistoryMigratorLogs.migratingDecisionDefinitionCompleted(c7Id);
    }
  }

  @Async
  public CompletableFuture<Void> migrateDecisionInstances() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_DECISION_INSTANCE);
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_DECISION_INSTANCE, idKeyDbModel -> {
        HistoricDecisionInstance historicDecisionInstance = c7Client.getHistoricDecisionInstance(idKeyDbModel.getC7Id());
        migrateDecisionInstance(historicDecisionInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricDecisionInstances(this::migrateDecisionInstance,
          dbClient.findLatestCreateTimeByType((HISTORY_DECISION_INSTANCE)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_DECISION_INSTANCE);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateDecisionInstance(HistoricDecisionInstance c7DecisionInstance) {
    if (c7DecisionInstance.getProcessDefinitionKey() == null) {
      // only migrate decision instances that were triggered by process definitions
      HistoryMigratorLogs.notMigratingDecisionInstancesNotOriginatingFromBusinessRuleTasks(c7DecisionInstance.getId());
      return;
    }

    String c7DecisionInstanceId = c7DecisionInstance.getId();
    if (shouldMigrate(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE)) {
      HistoryMigratorLogs.migratingDecisionInstance(c7DecisionInstanceId);

      if (!isMigrated(c7DecisionInstance.getDecisionDefinitionId(), HISTORY_DECISION_DEFINITION)) {
        markSkipped(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE, c7DecisionInstance.getEvaluationTime(), SKIP_REASON_MISSING_DECISION_DEFINITION);
        HistoryMigratorLogs.skippingDecisionInstanceDueToMissingDecisionDefinition(c7DecisionInstanceId);
        return;
      }

      if (!isMigrated(c7DecisionInstance.getProcessDefinitionId(), HISTORY_PROCESS_DEFINITION)) {
        markSkipped(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE, c7DecisionInstance.getEvaluationTime(), SKIP_REASON_MISSING_PROCESS_DEFINITION);
        HistoryMigratorLogs.skippingDecisionInstanceDueToMissingProcessDefinition(c7DecisionInstanceId);
        return;
      }

      if (!isMigrated(c7DecisionInstance.getProcessInstanceId(), HISTORY_PROCESS_INSTANCE)) {
        markSkipped(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE, c7DecisionInstance.getEvaluationTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingDecisionInstanceDueToMissingProcessInstance(c7DecisionInstanceId);
        return;
      }

      String c7RootDecisionInstanceId = c7DecisionInstance.getRootDecisionInstanceId();
      Long parentDecisionDefinitionKey = null;
      if (c7RootDecisionInstanceId != null) {
        if (!isMigrated(c7RootDecisionInstanceId, HISTORY_DECISION_INSTANCE)) {
          markSkipped(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE, c7DecisionInstance.getEvaluationTime(), SKIP_REASON_MISSING_PARENT_DECISION_INSTANCE);
          HistoryMigratorLogs.skippingDecisionInstanceDueToMissingParent(c7DecisionInstanceId);
          return;
        }
        parentDecisionDefinitionKey = findDecisionInstance(c7RootDecisionInstanceId).decisionDefinitionKey();
      }

      if (!isMigrated(c7DecisionInstance.getActivityInstanceId(), HISTORY_FLOW_NODE)) {
        markSkipped(c7DecisionInstanceId, TYPE.HISTORY_DECISION_INSTANCE, c7DecisionInstance.getEvaluationTime(), SKIP_REASON_MISSING_FLOW_NODE);
        HistoryMigratorLogs.skippingDecisionInstanceDueToMissingFlowNodeInstanceInstance(c7DecisionInstanceId);
        return;
      }

      DecisionDefinitionEntity decisionDefinition = findDecisionDefinition(
          c7DecisionInstance.getDecisionDefinitionId());
      Long processDefinitionKey = findProcessDefinitionKey(c7DecisionInstance.getProcessDefinitionId());
      Long processInstanceKey = findProcessInstanceByC7Id(
          c7DecisionInstance.getProcessInstanceId()).processInstanceKey();
      FlowNodeInstanceDbModel flowNode = findFlowNodeInstance(c7DecisionInstance.getActivityInstanceId());

      DecisionInstanceDbModel dbModel = decisionInstanceConverter.apply(c7DecisionInstance,
          decisionDefinition.decisionDefinitionKey(), processDefinitionKey,
          decisionDefinition.decisionRequirementsKey(), processInstanceKey, parentDecisionDefinitionKey,
          flowNode.flowNodeInstanceKey(), flowNode.flowNodeId());
      decisionInstanceMapper.insert(dbModel);
      markMigrated(c7DecisionInstanceId, dbModel.decisionInstanceKey(), c7DecisionInstance.getEvaluationTime(), HISTORY_DECISION_INSTANCE);
      HistoryMigratorLogs.migratingDecisionInstanceCompleted(c7DecisionInstanceId);
    }
  }

  @Async
  public CompletableFuture<Void> migrateIncidents() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_INCIDENT);
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_INCIDENT, idKeyDbModel -> {
        HistoricIncident historicIncident = c7Client.getHistoricIncident(idKeyDbModel.getC7Id());
        migrateIncident(historicIncident);
      });
    } else {
      c7Client.fetchAndHandleHistoricIncidents(this::migrateIncident, dbClient.findLatestCreateTimeByType((HISTORY_INCIDENT)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_INCIDENT);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateIncident(HistoricIncident c7Incident) {
    String c7IncidentId = c7Incident.getId();
    if (shouldMigrate(c7IncidentId, HISTORY_INCIDENT)) {
      HistoryMigratorLogs.migratingHistoricIncident(c7IncidentId);
      ProcessInstanceEntity c7ProcessInstance = findProcessInstanceByC7Id(c7Incident.getProcessInstanceId());
      if (c7ProcessInstance != null) {
        Long processInstanceKey = c7ProcessInstance.processInstanceKey();
        if (processInstanceKey != null) {
          Long flowNodeInstanceKey = findFlowNodeInstanceKey(c7Incident.getActivityId(), c7Incident.getProcessInstanceId());
          Long processDefinitionKey = findProcessDefinitionKey(c7Incident.getProcessDefinitionId());
          Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
          IncidentDbModel dbModel = incidentConverter.apply(c7Incident, processDefinitionKey, processInstanceKey, jobDefinitionKey, flowNodeInstanceKey);
          incidentMapper.insert(dbModel);
          markMigrated(c7IncidentId, dbModel.incidentKey(), c7Incident.getCreateTime(), HISTORY_INCIDENT);
          HistoryMigratorLogs.migratingHistoricIncidentCompleted(c7IncidentId);
        } else {
          markSkipped(c7IncidentId, HISTORY_INCIDENT, c7Incident.getCreateTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE_KEY);
          HistoryMigratorLogs.skippingHistoricIncident(c7IncidentId);
        }
      } else {
        markSkipped(c7IncidentId, HISTORY_INCIDENT, c7Incident.getCreateTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingHistoricIncident(c7IncidentId);
      }
    }
  }

  @Async
  public CompletableFuture<Void> migrateVariables() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_VARIABLE);

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_VARIABLE, idKeyDbModel -> {
        HistoricVariableInstance historicVariableInstance = c7Client.getHistoricVariableInstance(idKeyDbModel.getC7Id());
        migrateVariable(historicVariableInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricVariables(this::migrateVariable, dbClient.findLatestCreateTimeByType(HISTORY_VARIABLE));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_VARIABLE);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateVariable(HistoricVariableInstance c7Variable) {
    String c7VariableId = c7Variable.getId();
    if (shouldMigrate(c7VariableId, HISTORY_VARIABLE)) {
      HistoryMigratorLogs.migratingHistoricVariable(c7VariableId);

      String taskId = c7Variable.getTaskId();
      if (taskId != null && !isMigrated(taskId, HISTORY_USER_TASK)) {
        // Skip variable if it belongs to a skipped task
        markSkipped(c7VariableId, TYPE.HISTORY_VARIABLE, c7Variable.getCreateTime(), SKIP_REASON_BELONGS_TO_SKIPPED_TASK);
        HistoryMigratorLogs.skippingHistoricVariableDueToMissingTask(c7VariableId, taskId);
        return;
      }

      String c7ProcessInstanceId = c7Variable.getProcessInstanceId();
      if (isMigrated(c7ProcessInstanceId, HISTORY_PROCESS_INSTANCE)) {
        if (isMigrated(c7Variable.getActivityInstanceId(), HISTORY_FLOW_NODE) ||
            isMigrated(c7Variable.getActivityInstanceId(), HISTORY_PROCESS_INSTANCE)) {
          ProcessInstanceEntity processInstance = findProcessInstanceByC7Id(c7ProcessInstanceId);
          Long processInstanceKey = processInstance.processInstanceKey();
          Long scopeKey = findScopeKey(c7Variable.getActivityInstanceId());
          if (scopeKey != null) {
            VariableDbModel dbModel = variableConverter.apply(c7Variable, processInstanceKey, scopeKey);
            variableMapper.insert(dbModel);
            markMigrated(c7VariableId, dbModel.variableKey(), c7Variable.getCreateTime(), HISTORY_VARIABLE);
            HistoryMigratorLogs.migratingHistoricVariableCompleted(c7VariableId);
          } else {
            markSkipped(c7VariableId, TYPE.HISTORY_VARIABLE, c7Variable.getCreateTime(), SKIP_REASON_MISSING_SCOPE_KEY);
            HistoryMigratorLogs.skippingHistoricVariableDueToMissingScopeKey(c7VariableId);
          }
        } else {
          markSkipped(c7VariableId, TYPE.HISTORY_VARIABLE, c7Variable.getCreateTime(), SKIP_REASON_MISSING_FLOW_NODE);
          HistoryMigratorLogs.skippingHistoricVariableDueToMissingFlowNode(c7VariableId);
        }
      } else {
        markSkipped(c7VariableId, TYPE.HISTORY_VARIABLE, c7Variable.getCreateTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingHistoricVariableDueToMissingProcessInstance(c7VariableId);
      }
    }
  }

  @Async
  public CompletableFuture<Void> migrateUserTasks() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_USER_TASK);

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_USER_TASK, idKeyDbModel -> {
        HistoricTaskInstance historicTaskInstance = c7Client.getHistoricTaskInstance(idKeyDbModel.getC7Id());
        migrateUserTask(historicTaskInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricUserTasks(this::migrateUserTask, dbClient.findLatestCreateTimeByType((HISTORY_USER_TASK)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_USER_TASK);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateUserTask(HistoricTaskInstance c7UserTask) {
    String c7UserTaskId = c7UserTask.getId();
    if (shouldMigrate(c7UserTaskId, HISTORY_USER_TASK)) {
      HistoryMigratorLogs.migratingHistoricUserTask(c7UserTaskId);
      if (isMigrated(c7UserTask.getProcessInstanceId(), HISTORY_PROCESS_INSTANCE)) {
        ProcessInstanceEntity processInstance = findProcessInstanceByC7Id(c7UserTask.getProcessInstanceId());
        if (isMigrated(c7UserTask.getActivityInstanceId(), HISTORY_FLOW_NODE)) {
          Long elementInstanceKey = findFlowNodeInstanceKey(c7UserTask.getActivityInstanceId());
          Long processDefinitionKey = findProcessDefinitionKey(c7UserTask.getProcessDefinitionId());
          UserTaskDbModel dbModel = userTaskConverter.apply(c7UserTask, processDefinitionKey, processInstance, elementInstanceKey);
          userTaskMapper.insert(dbModel);
          markMigrated(c7UserTaskId, dbModel.userTaskKey(), c7UserTask.getStartTime(), HISTORY_USER_TASK);
          HistoryMigratorLogs.migratingHistoricUserTaskCompleted(c7UserTaskId);
        } else {
          markSkipped(c7UserTaskId, TYPE.HISTORY_USER_TASK, c7UserTask.getStartTime(), SKIP_REASON_MISSING_FLOW_NODE);
          HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingFlowNode(c7UserTaskId);
        }
      } else {
        markSkipped(c7UserTaskId, TYPE.HISTORY_USER_TASK, c7UserTask.getStartTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingHistoricUserTaskDueToMissingProcessInstance(c7UserTaskId);
      }
    }
  }

  @Async
  public CompletableFuture<Void> migrateFlowNodes() {
    HistoryMigratorLogs.startingMigrationForType(HISTORY_FLOW_NODE);

    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.fetchAndHandleSkippedForType(HISTORY_FLOW_NODE, idKeyDbModel -> {
        HistoricActivityInstance historicActivityInstance = c7Client.getHistoricActivityInstance(idKeyDbModel.getC7Id());
        migrateFlowNode(historicActivityInstance);
      });
    } else {
      c7Client.fetchAndHandleHistoricFlowNodes(this::migrateFlowNode, dbClient.findLatestCreateTimeByType((HISTORY_FLOW_NODE)));
    }
    HistoryMigratorLogs.finishedMigrationForType(HISTORY_FLOW_NODE);
    return CompletableFuture.completedFuture(null);
  }

  private void migrateFlowNode(HistoricActivityInstance c7FlowNode) {
    String c7FlowNodeId = c7FlowNode.getId();
    if (shouldMigrate(c7FlowNodeId, HISTORY_FLOW_NODE)) {
      HistoryMigratorLogs.migratingHistoricFlowNode(c7FlowNodeId);
      ProcessInstanceEntity processInstance = findProcessInstanceByC7Id(c7FlowNode.getProcessInstanceId());
      if (processInstance != null) {
        Long processInstanceKey = processInstance.processInstanceKey();
        Long processDefinitionKey = findProcessDefinitionKey(c7FlowNode.getProcessDefinitionId());
        FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(c7FlowNode, processDefinitionKey, processInstanceKey);
        flowNodeMapper.insert(dbModel);
        markMigrated(c7FlowNodeId, dbModel.flowNodeInstanceKey(), c7FlowNode.getStartTime(), HISTORY_FLOW_NODE);
        HistoryMigratorLogs.migratingHistoricFlowNodeCompleted(c7FlowNodeId);
      } else {
        markSkipped(c7FlowNodeId, HISTORY_FLOW_NODE, c7FlowNode.getStartTime(), SKIP_REASON_MISSING_PROCESS_INSTANCE);
        HistoryMigratorLogs.skippingHistoricFlowNode(c7FlowNodeId);
      }
    }
  }

  protected ProcessInstanceEntity findProcessInstanceByC7Id(String processInstanceId) {
    if (processInstanceId == null)
      return null;

    Long c8Key = dbClient.findC8KeyByC7IdAndType(processInstanceId, HISTORY_PROCESS_INSTANCE);
    if (c8Key == null) {
      return null;
    }

    return processInstanceMapper.findOne(c8Key);
  }

  protected DecisionInstanceEntity findDecisionInstance(String decisionInstanceId) {
    if (decisionInstanceId == null)
      return null;

    Long key = dbClient.findC8KeyByC7IdAndType(decisionInstanceId, HISTORY_DECISION_INSTANCE);
    if (key == null) {
      return null;
    }

    return decisionInstanceMapper.search(
            DecisionInstanceDbQuery.of(b -> b.filter(value -> value.decisionInstanceKeys(key))))
        .stream()
        .findFirst()
        .orElse(null);
  }

  protected DecisionDefinitionEntity findDecisionDefinition(String decisionDefinitionId) {
    Long key = dbClient.findC8KeyByC7IdAndType(decisionDefinitionId, HISTORY_DECISION_DEFINITION);
    if (key == null) {
      return null;
    }

    return decisionDefinitionMapper.search(
            DecisionDefinitionDbQuery.of(b -> b.filter(value -> value.decisionDefinitionKeys(key))))
        .stream()
        .findFirst()
        .orElse(null);
  }

  private Long findProcessDefinitionKey(String processDefinitionId) {
    Long key = dbClient.findC8KeyByC7IdAndType(processDefinitionId, HISTORY_PROCESS_DEFINITION);
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

  private Long findFlowNodeInstanceKey(String activityId, String processInstanceId) {
    Long key = dbClient.findC8KeyByC7IdAndType(processInstanceId, HISTORY_PROCESS_INSTANCE);
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

  protected Long findFlowNodeInstanceKey(String activityInstanceId) {
    return Optional.ofNullable(findFlowNodeInstance(activityInstanceId))
        .map(FlowNodeInstanceDbModel::flowNodeInstanceKey)
        .orElse(null);
  }

  protected FlowNodeInstanceDbModel findFlowNodeInstance(String activityInstanceId) {
    Long key = dbClient.findC8KeyByC7IdAndType(activityInstanceId, HISTORY_FLOW_NODE);
    if (key == null) {
      return null;
    }

    return flowNodeMapper.search(FlowNodeInstanceDbQuery.of(b -> b.filter(f -> f.flowNodeInstanceKeys(key))))
        .stream()
        .findFirst()
        .orElse(null);
  }

  private Long findScopeKey(String instanceId) {
    Long key = findFlowNodeInstanceKey(instanceId);
    if (key != null) {
      return key;
    }

    Long processInstanceKey = dbClient.findC8KeyByC7IdAndType(instanceId, HISTORY_PROCESS_INSTANCE);
    if (processInstanceKey == null) {
      return null;
    }

    List<ProcessInstanceEntity> processInstances = processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b -> b.filter(value -> value.processInstanceKeys(processInstanceKey))));
    return processInstances.isEmpty() ? null : processInstanceKey;
  }

  private boolean isMigrated(String id, TYPE type) {
    return dbClient.checkHasC8KeyByC7IdAndType(id, type);
  }

  private boolean shouldMigrate(String id, TYPE type) {
    if (mode == RETRY_SKIPPED) {
      return !dbClient.checkHasC8KeyByC7IdAndType(id, type);
    }
    return !dbClient.checkExistsByC7IdAndType(id, type);
  }

  protected void markMigrated(String c7Id, Long c8Key, Date createTime, TYPE type) {
    saveRecord(c7Id, c8Key, type, createTime, null);
  }

  protected void markSkipped(String c7Id, TYPE type, Date createTime, String skipReason) {
    saveRecord(c7Id, null, type, createTime, skipReason);
  }

  protected void saveRecord(String c7Id, Long c8Key, TYPE type, Date createTime, String skipReason) {
    if (RETRY_SKIPPED.equals(mode)) {
      dbClient.updateC8KeyByC7IdAndType(c7Id, c8Key, type);
    } else if (MIGRATE.equals(mode)) {
      dbClient.insert(c7Id, c8Key, createTime, type, skipReason);

    }
  }

  public void setMode(MigratorMode mode) {
    this.mode = mode;
  }

}
