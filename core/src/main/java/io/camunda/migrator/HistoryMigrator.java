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
import static io.camunda.migrator.impl.util.ExceptionUtils.callApi;

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
import io.camunda.migrator.impl.Pagination;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessDefinitionEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import java.util.Date;
import java.util.List;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.impl.HistoricActivityInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricIncidentQueryImpl;
import org.camunda.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricVariableInstanceQueryImpl;
import org.camunda.bpm.engine.impl.ProcessDefinitionQueryImpl;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionDefinitionQuery;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(C8DataSourceConfigured.class)
public class HistoryMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(HistoryMigrator.class);

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
  private HistoryService historyService;

  @Autowired
  private RepositoryService repositoryService;

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
    LOGGER.info("Migrating C7 data...");
    // Start process instance
    migrateProcessDefinitions();
    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();

    //migrateDecisionDefinitions();
  }

  private void migrateDecisionDefinitions() {
    LOGGER.info("Migrating decision definitions");

    DecisionDefinitionQuery legacyDecisionDefinitionQuery = repositoryService.createDecisionDefinitionQuery()
        .orderByDeploymentTime()
        .asc()
        .orderByDecisionDefinitionId()
        .asc();

    Date deployedAfter = getLatestStartDateByType(HISTORY_DECISION_DEFINITION);
    if (deployedAfter != null) {
      legacyDecisionDefinitionQuery.deployedAfter(deployedAfter);
    }

    new Pagination<DecisionDefinition>().pageSize(getPageSize())
        .query(legacyDecisionDefinitionQuery)
        .callback(legacyDecisionDefinition -> {
          String legacyId = legacyDecisionDefinition.getId();
          LOGGER.debug("Migrating legacy decision definition with id: [{}]", legacyId);
          DecisionDefinitionDbModel dbModel = decisionDefinitionConverter.apply(legacyDecisionDefinition);
          decisionDefinitionMapper.insert(dbModel);
          Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyDecisionDefinition.getDeploymentId());
          dbClient.insert(legacyId, deploymentTime, dbModel.decisionDefinitionKey(), HISTORY_DECISION_DEFINITION);
          LOGGER.debug("Migration of legacy decision definition with id [{}] completed", legacyId);
        });
  }

  private void migrateProcessDefinitions() {
    LOGGER.info("Migrating process definitions");

    ProcessDefinitionQueryImpl legacyProcessDefinitionQuery = (ProcessDefinitionQueryImpl) repositoryService.createProcessDefinitionQuery()
        .orderByDeploymentTime()
        .asc()
        .orderByProcessDefinitionId()
        .asc();

    Date deployedAfter = getLatestStartDateByType(HISTORY_PROCESS_DEFINITION);
    if (deployedAfter != null) {
      legacyProcessDefinitionQuery.deployedAfter(deployedAfter);
    }

    new Pagination<ProcessDefinition>().pageSize(getPageSize())
        .query(legacyProcessDefinitionQuery)
        .callback(legacyProcessDefinition -> {
          String legacyId = legacyProcessDefinition.getId();
          if (!dbClient.checkExists(legacyId)) {
            LOGGER.debug("Migrating legacy process definition with id: [{}]", legacyId);
            ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(legacyProcessDefinition);
            processDefinitionMapper.insert(dbModel);
            Date deploymentTime = c7Client.getDefinitionDeploymentTime(legacyProcessDefinition.getDeploymentId());
            dbClient.insert(legacyId, deploymentTime, dbModel.processDefinitionKey(), HISTORY_PROCESS_DEFINITION);
            LOGGER.debug("Migration of legacy process definition with id [{}] completed", legacyId);
          }
        });
  }

  private void migrateProcessInstances() {
    LOGGER.info("Migrating process instances");

    HistoricProcessInstanceQueryImpl legacyProcessInstanceQuery = (HistoricProcessInstanceQueryImpl) historyService.createHistoricProcessInstanceQuery()
        .orderByProcessInstanceStartTime()
        .asc()
        .orderByProcessInstanceId()
        .asc();

    Date startedAfter = getLatestStartDateByType(HISTORY_PROCESS_INSTANCE);
    if (startedAfter != null) {
      legacyProcessInstanceQuery.startedAfter(startedAfter);
    }

    new Pagination<HistoricProcessInstance>().pageSize(getPageSize())
        .query(legacyProcessInstanceQuery)
        .callback(legacyProcessInstance -> {
          String legacyProcessInstanceId = legacyProcessInstance.getId();
          if (!dbClient.checkExists(legacyProcessInstanceId)) {
            LOGGER.debug("Migrating legacy process instance with id: [{}]", legacyProcessInstanceId);
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
                ProcessInstanceDbModel dbModel = processInstanceConverter.apply(legacyProcessInstance,
                    processDefinitionKey, parentProcessInstanceKey);
                processInstanceMapper.insert(dbModel);
                dbClient.insert(legacyProcessInstanceId, legacyProcessInstance.getStartTime(),
                    dbModel.processInstanceKey(), HISTORY_PROCESS_INSTANCE);
                LOGGER.debug("Migration of legacy process instances with id [{}] completed", legacyProcessInstanceId);
              } else {
                LOGGER.debug(
                    "Migration of legacy process instance with id [{}] skipped. Parent process instance not yet available.",
                    legacyProcessInstanceId);
              }
            } else {
              LOGGER.debug(
                  "Migration of legacy process instance with id [{}] skipped. Process definition not yet available.",
                  legacyProcessInstanceId);
            }
          }
        });
  }

  private void migrateIncidents() {
    LOGGER.info("Migrating incidents");

    HistoricIncidentQueryImpl legacyIncidentQuery = (HistoricIncidentQueryImpl) historyService.createHistoricIncidentQuery()
        .orderByCreateTime()
        .asc()
        .orderByIncidentId()
        .asc();

    Date createdAfter = getLatestStartDateByType(HISTORY_INCIDENT);
    if (createdAfter != null) {
      legacyIncidentQuery.createTimeAfter(createdAfter);
    }

    new Pagination<HistoricIncident>().pageSize(getPageSize()).query(legacyIncidentQuery).callback(legacyIncident -> {
      String legacyIncidentId = legacyIncident.getId();
      if (!dbClient.checkExists(legacyIncidentId)) {
        LOGGER.debug("Migrating legacy incident with id: [{}]", legacyIncidentId);
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
            dbClient.insert(legacyIncidentId, legacyIncident.getCreateTime(), dbModel.incidentKey(),
                HISTORY_INCIDENT);
            LOGGER.debug("Migration of legacy incident with id [{}] completed.", legacyIncidentId);
          } else {
            LOGGER.debug("Migration of legacy incident with id [{}] skipped. Process instance not yet available.",
                legacyIncidentId);
          }
        }
      }
    });
  }

  private void migrateVariables() {
    LOGGER.info("Migrating variables");

    HistoricVariableInstanceQueryImpl legacyVariableQuery = (HistoricVariableInstanceQueryImpl) historyService.createHistoricVariableInstanceQuery()
        .orderByVariableId()
        .asc();

    String latestLegacyId = dbClient.findLatestIdByType(HISTORY_VARIABLE);
    if (latestLegacyId != null) {
      legacyVariableQuery.idAfter(latestLegacyId);
    }

    new Pagination<HistoricVariableInstance>().pageSize(getPageSize())
        .query(legacyVariableQuery)
        .callback(legacyVariable -> {
          String legacyVariableId = legacyVariable.getId();
          if (!dbClient.checkExists(legacyVariableId)) {
            LOGGER.debug("Migrating legacy variable with id: [{}]", legacyVariableId);
            String legacyProcessInstanceId = legacyVariable.getProcessInstanceId();
            ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyProcessInstanceId);

            if (processInstance != null) {
              Long processInstanceKey = processInstance.processInstanceKey();
              Long scopeKey = findFlowNodeKey(
                  legacyVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
              if (scopeKey != null) {
                VariableDbModel dbModel = variableConverter.apply(legacyVariable, processInstanceKey, scopeKey);
                variableMapper.insert(dbModel);
                dbClient.insert(legacyVariableId, legacyVariable.getCreateTime(), dbModel.variableKey(),
                    HISTORY_VARIABLE);
                LOGGER.debug("Migration of legacy variable with id [{}] completed.", legacyVariableId);
              } else {
                LOGGER.debug("Migration of legacy variable with id [{}] skipped. Activity instance not yet available.",
                    legacyVariableId);
              }
            } else {
              LOGGER.debug("Migration of legacy variable with id [{}] skipped. Process instance not yet available.",
                  legacyVariableId);
            }
          }
        });
  }

  private void migrateUserTasks() {
    LOGGER.info("Migrating user tasks");

    HistoricTaskInstanceQueryImpl legacyTaskQuery = (HistoricTaskInstanceQueryImpl) historyService.createHistoricTaskInstanceQuery()
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .orderByTaskId()
        .asc();

    Date startedAfter = getLatestStartDateByType(HISTORY_USER_TASK);
    if (startedAfter != null) {
      legacyTaskQuery.startedAfter(startedAfter);
    }

    new Pagination<HistoricTaskInstance>().pageSize(getPageSize()).query(legacyTaskQuery).callback(legacyUserTask -> {
      String legacyUserTaskId = legacyUserTask.getId();
      if (!dbClient.checkExists(legacyUserTaskId)) {
        LOGGER.debug("Migrating legacy user task with id: [{}]", legacyUserTaskId);
        ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(legacyUserTask.getProcessInstanceId());
        if (processInstance != null) {
          Long elementInstanceKey = findFlowNodeKey(legacyUserTask.getActivityInstanceId());
          if (elementInstanceKey != null) {
            Long processDefinitionKey = findProcessDefinitionKey(legacyUserTask.getProcessDefinitionId());
            UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstance,
                elementInstanceKey);
            userTaskMapper.insert(dbModel);
            dbClient.insert(legacyUserTaskId, legacyUserTask.getStartTime(), dbModel.userTaskKey(),
                HISTORY_USER_TASK);
            LOGGER.debug("Migration of legacy user task with id [{}] completed.", legacyUserTaskId);
          } else {
            LOGGER.debug("Migration of legacy user task with id [{}] skipped. Flow node instance yet not available.",
                legacyUserTaskId);
          }
        } else {
          LOGGER.debug("Migration of legacy user task with id [{}] skipped. Process instance [{}] not yet available.",
              legacyUserTaskId, legacyUserTask.getProcessInstanceId());
        }
      }
    });
  }

  private void migrateFlowNodes() {
    LOGGER.info("Migrating flow nodes");

    HistoricActivityInstanceQueryImpl legacyFlowNodeQuery = (HistoricActivityInstanceQueryImpl) historyService.createHistoricActivityInstanceQuery()
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .orderByHistoricActivityInstanceId()
        .asc();

    Date startedAfter = getLatestStartDateByType(HISTORY_FLOW_NODE);
    if (startedAfter != null) {
      legacyFlowNodeQuery.startedAfter(startedAfter);
    }

    new Pagination<HistoricActivityInstance>().pageSize(getPageSize())
        .query(legacyFlowNodeQuery)
        .callback(legacyFlowNode -> {
          String legacyFlowNodeId = legacyFlowNode.getId();
          if (!dbClient.checkExists(legacyFlowNodeId)) {
            LOGGER.debug("Migrating legacy flow node with id: [{}]", legacyFlowNodeId);
            ProcessInstanceEntity processInstance = findProcessInstanceByLegacyId(
                legacyFlowNode.getProcessInstanceId());
            if (processInstance != null) {
              Long processInstanceKey = processInstance.processInstanceKey();
              Long processDefinitionKey = findProcessDefinitionKey(legacyFlowNode.getProcessDefinitionId());
              FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(legacyFlowNode, processDefinitionKey,
                  processInstanceKey);
              flowNodeMapper.insert(dbModel);
              dbClient.insert(legacyFlowNodeId, legacyFlowNode.getStartTime(), dbModel.flowNodeInstanceKey(),
                  HISTORY_FLOW_NODE);
              LOGGER.debug("Migration of legacy flow node with id [{}] completed.", legacyFlowNodeId);
            } else {
              LOGGER.debug("Migration of legacy flow node with id [{}] skipped. Process instance not yet available.",
                  legacyFlowNodeId);
            }
          }
        });
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

  protected Date getLatestStartDateByType(IdKeyMapper.TYPE type) {
    Date latestStartDate = dbClient.findLatestStartDateByType(type);
    LOGGER.debug("Latest start date for {}: {}", type, latestStartDate);
    return latestStartDate;
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

  public int getPageSize() {
    return migratorProperties.getPageSize();
  }
}
