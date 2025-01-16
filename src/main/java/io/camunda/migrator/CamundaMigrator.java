package io.camunda.migrator;

import io.camunda.db.rdbms.read.domain.*;
import io.camunda.db.rdbms.sql.*;
import io.camunda.db.rdbms.write.domain.*;
import io.camunda.migrator.converter.*;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import io.camunda.search.filter.ProcessDefinitionFilter;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMigrator.class);

  private final ProcessInstanceMapper processInstanceMapper;
  private final FlowNodeInstanceMapper flowNodeMapper;
  private final UserTaskMapper userTaskMapper;
  private final VariableMapper variableMapper;
  private final IncidentMapper incidentMapper;
  private final ProcessDefinitionMapper processDefinitionMapper;

  private final RuntimeService runtimeService;
  private final HistoryService historyService;
  private final RepositoryService repositoryService;

  private final ProcessInstanceConverter processInstanceConverter;
  private final FlowNodeConverter flowNodeConverter;
  private final UserTaskConverter userTaskConverter;
  private final VariableConverter variableConverter;
  private final IncidentConverter incidentConverter;
  private final ProcessDefinitionConverter processDefinitionConverter;

  public CamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      FlowNodeInstanceMapper flowNodeMapper,
      UserTaskMapper userTaskMapper,
      VariableMapper variableMapper,
      IncidentMapper incidentMapper,
      ProcessDefinitionMapper processDefinitionMapper,

      RuntimeService runtimeService,
      HistoryService historyService,
      RepositoryService repositoryService,

      ProcessInstanceConverter processInstanceConverter,
      FlowNodeConverter flowNodeConverter,
      UserTaskConverter userTaskConverter,
      VariableConverter variableConverter,
      IncidentConverter incidentConverter,
      ProcessDefinitionConverter processDefinitionConverter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.flowNodeMapper = flowNodeMapper;
    this.userTaskMapper = userTaskMapper;
    this.variableMapper = variableMapper;
    this.incidentMapper = incidentMapper;
    this.processDefinitionMapper = processDefinitionMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.repositoryService = repositoryService;
    this.processInstanceConverter = processInstanceConverter;
    this.flowNodeConverter = flowNodeConverter;
    this.userTaskConverter = userTaskConverter;
    this.variableConverter = variableConverter;
    this.incidentConverter = incidentConverter;
    this.processDefinitionConverter = processDefinitionConverter;
  }

  public void migrateAllHistoricProcessInstances() {
    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    runtimeService.startProcessInstanceByKey("simple-process-user-task");
    runtimeService.startProcessInstanceByKey("simple-process-user-task-with-variables");

    addIncidentToProcess("simple-process-user-task", "failedJob");

    migrateProcessDefinitions();
    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();
  }

  private void migrateProcessDefinitions() {
    repositoryService.createProcessDefinitionQuery().list().forEach(legacyProcessDefinition -> {
      String processDefinitionId = legacyProcessDefinition.getId();
      if (checkProcessDefinitionNotMigrated(processDefinitionId)) {
        LOGGER.info("Migration of legacy process definition with id '{}' completed", processDefinitionId);
        ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(legacyProcessDefinition);
        processDefinitionMapper.insert(dbModel);
      } else {
        LOGGER.info("Legacy process definition with id '{}' has been migrated already. Skipping.", processDefinitionId);
      }
    });
  }

  private void migrateProcessInstances() {
    historyService.createHistoricProcessInstanceQuery().list().forEach(legacyProcessInstance -> {
        String legacyProcessInstanceId = legacyProcessInstance.getId();
        if (checkProcessInstanceNotMigrated(legacyProcessInstanceId)) {

          Long processDefinitionKey = findProcessDefinitionKey(legacyProcessInstance.getProcessDefinitionId());
          if (processDefinitionKey != null) {
            String legacySuperProcessInstanceId = legacyProcessInstance.getSuperProcessInstanceId();
            Long parentProcessInstanceKey = null;
            if (legacySuperProcessInstanceId != null) {
              parentProcessInstanceKey = findProcessInstanceKey(legacySuperProcessInstanceId).processInstanceKey();
            }

            if (parentProcessInstanceKey != null
                // Continue if PI has no parent.
                || legacySuperProcessInstanceId == null) {
              LOGGER.info("Migration of legacy process instances with id '{}' completed", legacyProcessInstanceId);
              ProcessInstanceDbModel dbModel = processInstanceConverter.apply(legacyProcessInstance, processDefinitionKey, parentProcessInstanceKey);
              processInstanceMapper.insert(dbModel);
            } else {
              LOGGER.info("Migration of legacy process instance with id '{}' skipped. Parent process instance not yet available.", legacyProcessInstanceId);
            }
          } else {
            LOGGER.info("Migration of legacy process instance with id '{}' skipped. Process definition not yet available.", legacyProcessInstanceId);
          }
        } else {
          LOGGER.info("Legacy process instances with id '{}' has been migrated already. Skipping.", legacyProcessInstanceId);
        }
    });
  }

  private void migrateIncidents() {
    historyService.createHistoricIncidentQuery().list().forEach(legacyIncident -> {
      String legacyIncidentId = legacyIncident.getId();
      if (checkIncidentNotMigrated(legacyIncidentId)) {
        Long processInstanceKey = findProcessInstanceKey(legacyIncident.getProcessInstanceId()).processInstanceKey();
        if (processInstanceKey != null) {
          Long flowNodeInstanceKey = findFlowNodeKey(legacyIncident.getActivityId(), legacyIncident.getProcessInstanceId());
          if (flowNodeInstanceKey != null) {
            LOGGER.info("Migration of legacy incident with id '{}' completed.", legacyIncidentId);
            Long processDefinitionKey = findProcessDefinitionKey(legacyIncident.getProcessDefinitionId());
            Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
            IncidentDbModel dbModel = incidentConverter.apply(legacyIncident, processDefinitionKey, processInstanceKey, jobDefinitionKey, flowNodeInstanceKey);
            incidentMapper.insert(dbModel);
          } else {
            LOGGER.info("Migration of legacy incident with id '{}' skipped. Flow node not yet available.", legacyIncidentId);
          }
        } else {
          LOGGER.info("Migration of legacy incident with id '{}' skipped. Process instance not yet available.", legacyIncidentId);
        }
      } else {
        LOGGER.info("Legacy incident with id '{}' has been migrated already. Skipping.", legacyIncidentId);
      }
    });
  }

  private void migrateVariables() {
    historyService.createHistoricVariableInstanceQuery().list().forEach(legacyVariable -> {
      String legacyVariableId = legacyVariable.getId();
      if (checkVariableNotMigrated(legacyVariableId)) {
        Long processInstanceKey = findProcessInstanceKey(legacyVariable.getProcessInstanceId()).processInstanceKey();
        if (processInstanceKey != null) {
          Long scopeKey = findFlowNodeKey(legacyVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
          if (scopeKey != null) {
            LOGGER.info("Migration of legacy variable with id '{}' completed.", legacyVariableId);
            VariableDbModel dbModel = variableConverter.apply(legacyVariable, processInstanceKey, scopeKey);
            variableMapper.insert(dbModel);
          } else {
            LOGGER.info("Migration of legacy variable with id '{}' skipped. Activity instance not yet available.", legacyVariableId);
          }
        } else {
          LOGGER.info("Migration of legacy variable with id '{}' skipped. Process instance not yet available.", legacyVariableId);
        }
      } else {
        LOGGER.info("Legacy variable with id '{}' has been migrated already. Skipping.", legacyVariableId);
      }
    });
  }

  private void migrateUserTasks() {
    historyService.createHistoricTaskInstanceQuery().list().forEach(legacyUserTask -> {
      String legacyUserTaskId = legacyUserTask.getId();
      if (checkUserTaskNotMigrated(legacyUserTaskId)) {
        ProcessInstanceDbModel processInstance = findProcessInstanceKey(legacyUserTask.getProcessInstanceId());
        if (processInstance != null) {
          Long elementInstanceKey = findFlowNodeKey(legacyUserTask.getActivityInstanceId());
          if (elementInstanceKey != null) {
            LOGGER.info("Migration of legacy user task with id '{}' completed.", legacyUserTaskId);
            Long processDefinitionKey = findProcessDefinitionKey(legacyUserTask.getProcessDefinitionId());
            UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstance, elementInstanceKey);
            userTaskMapper.insert(dbModel);
          } else {
            LOGGER.info("Migration of legacy user task with id '{}' skipped. Flow node instance yet not available.", legacyUserTaskId);
          }
        } else {
          LOGGER.info("Migration of legacy user task with id '{}' skipped. Process instance not yet available.", legacyUserTaskId);
        }
      } else {
        LOGGER.info("Legacy user task with id '{}' has been migrated already. Skipping.", legacyUserTaskId);
      }
    });
  }

  private void migrateFlowNodes() {
    historyService.createHistoricActivityInstanceQuery().list().forEach(legacyFlowNode -> {
      String legacyFlowNodeId = legacyFlowNode.getId();
      if (checkFlowNodeNotMigrated(legacyFlowNodeId)) {
        Long processInstanceKey = findProcessInstanceKey(legacyFlowNode.getProcessInstanceId()).processInstanceKey();
        if (processInstanceKey != null) {
          LOGGER.info("Migration of legacy flow node with id '{}' completed.", legacyFlowNodeId);
          Long processDefinitionKey = findProcessDefinitionKey(legacyFlowNode.getProcessDefinitionId());
          FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(legacyFlowNode, processDefinitionKey, processInstanceKey);
          flowNodeMapper.insert(dbModel);
        } else {
          LOGGER.info("Migration of legacy flow node with id '{}' skipped. Process instance not yet available.", legacyFlowNodeId);
        }
      } else {
        LOGGER.info("Legacy flow node with id '{}' has been migrated already. Skipping.", legacyFlowNodeId);
      }
    });
  }

  protected ProcessInstanceDbModel findProcessInstanceKey(String processInstanceId) {
    if (processInstanceId == null) return null;

    List<ProcessInstanceDbModel> processInstances = processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b -> b.legacyProcessInstanceId(processInstanceId)));

    if (!processInstances.isEmpty()) {
      return processInstances.get(0);
    } else {
      return null;
    }
  }

  private Long findProcessDefinitionKey(String processDefinitionId) {
    List<ProcessDefinitionDbModel> processDefinitions = processDefinitionMapper.search(
        ProcessDefinitionDbQuery.of(b -> b.legacyId(processDefinitionId)));

    if (!processDefinitions.isEmpty()) {
      return processDefinitions.get(0).processDefinitionKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityId, String processInstanceId) {
    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b ->
            b.filter(FlowNodeInstanceFilter.of(f -> f.flowNodeIds(activityId)))
                .legacyProcessInstanceId(processInstanceId)));

    if (!flowNodes.isEmpty()) {
      return flowNodes.get(0).flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityInstanceId) {
    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b -> b.legacyId(activityInstanceId)));

    if (!flowNodes.isEmpty()) {
      return flowNodes.get(0).flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  protected boolean checkProcessDefinitionNotMigrated(String legacyProcessDefinitionId) {
    return processDefinitionMapper.search(
        ProcessDefinitionDbQuery.of(b ->
            b.legacyId(legacyProcessDefinitionId))).isEmpty();
  }

  protected boolean checkProcessInstanceNotMigrated(String legacyProcessInstanceId) {
    return processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b ->
            b.legacyProcessInstanceId(legacyProcessInstanceId))).isEmpty();
  }

  protected boolean checkIncidentNotMigrated(String legacyId) {
    return incidentMapper.search(
        IncidentDbQuery.of(b ->
            b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkVariableNotMigrated(String legacyId) {
    return variableMapper.search(
        VariableDbQuery.of(b ->
            b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkUserTaskNotMigrated(String legacyId) {
    return userTaskMapper.search(
        UserTaskDbQuery.of(b ->
            b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkFlowNodeNotMigrated(String legacyId) {
    return flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b ->
            b.legacyId(legacyId))).isEmpty();
  }

  private void addIncidentToProcess(String processInstanceId, String incidentType) {
    Execution processInstance = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(processInstanceId)
        .listPage(0, 1).get(0);

    runtimeService.createIncident(incidentType, processInstance.getId(), "someConfig", "The message of failure");
  }

}
