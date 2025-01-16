package io.camunda.migrator;

import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.IncidentDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.UserTaskDbQuery;
import io.camunda.db.rdbms.read.domain.VariableDbQuery;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.converter.FlowNodeConverter;
import io.camunda.migrator.converter.IncidentConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.migrator.converter.UserTaskConverter;
import io.camunda.migrator.converter.VariableConverter;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import org.camunda.bpm.engine.HistoryService;
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

  private final RuntimeService runtimeService;
  private final HistoryService historyService;

  private final ProcessInstanceConverter processInstanceConverter;
  private final FlowNodeConverter flowNodeConverter;
  private final UserTaskConverter userTaskConverter;
  private final VariableConverter variableConverter;
  private final IncidentConverter incidentConverter;

  public CamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      FlowNodeInstanceMapper flowNodeMapper,
      UserTaskMapper userTaskMapper,
      VariableMapper variableMapper,
      IncidentMapper incidentMapper,

      RuntimeService runtimeService,
      HistoryService historyService,

      ProcessInstanceConverter processInstanceConverter,
      FlowNodeConverter flowNodeConverter,
      UserTaskConverter userTaskConverter,
      VariableConverter variableConverter,
      IncidentConverter incidentConverter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.flowNodeMapper = flowNodeMapper;
    this.userTaskMapper = userTaskMapper;
    this.variableMapper = variableMapper;
    this.incidentMapper = incidentMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.processInstanceConverter = processInstanceConverter;
    this.flowNodeConverter = flowNodeConverter;
    this.userTaskConverter = userTaskConverter;
    this.variableConverter = variableConverter;
    this.incidentConverter = incidentConverter;
  }

  public void migrateAllHistoricProcessInstances() {
    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    runtimeService.startProcessInstanceByKey("simple-process-user-task");
    runtimeService.startProcessInstanceByKey("simple-process-user-task-with-variables");

    addIncidentToProcess("simple-process-user-task", "failedJob");

    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();
  }

  private void migrateProcessInstances() {
    historyService.createHistoricProcessInstanceQuery().list().forEach(historicProcessInstance -> {
      String legacyProcessInstanceId = historicProcessInstance.getId();
      if (checkProcessInstanceNotMigrated(legacyProcessInstanceId)) {

        String legacySuperProcessInstanceId = historicProcessInstance.getSuperProcessInstanceId();
        Long parentProcessInstanceKey = null;
        if (legacySuperProcessInstanceId != null) {
          parentProcessInstanceKey = findProcessInstanceKey(legacySuperProcessInstanceId);
        }

        // Continue if PI has no parent.
        if (parentProcessInstanceKey != null || legacySuperProcessInstanceId == null) {
          LOGGER.info("Migration of legacy process instances with id '{}' completed", legacyProcessInstanceId);
          Long processDefinitionKey = null; // TODO migrate process definitions
          ProcessInstanceDbModel dbModel = processInstanceConverter.apply(historicProcessInstance, processDefinitionKey, parentProcessInstanceKey);
          processInstanceMapper.insert(dbModel);
        } else {
          LOGGER.info("Migration of legacy process instance with id '{}' skipped. Parent process instance not yet available.", legacyProcessInstanceId);
        }
      } else {
        LOGGER.info("Legacy process instances with id '{}' has been migrated already. Skipping.", legacyProcessInstanceId);
      }
    });
  }

  private void migrateIncidents() {
    historyService.createHistoricIncidentQuery().list().forEach(historicIncident -> {
      String legacyIncidentId = historicIncident.getId();
      if (checkIncidentNotMigrated(legacyIncidentId)) {
        Long processInstanceKey = findProcessInstanceKey(historicIncident.getProcessInstanceId());
        if (processInstanceKey != null) {
          Long flowNodeInstanceKey = findFlowNodeKey(historicIncident.getActivityId(), historicIncident.getProcessInstanceId());
          if (flowNodeInstanceKey != null) {
            LOGGER.info("Migration of legacy incident with id '{}' completed.", legacyIncidentId);
            Long processDefinitionKey = null; // TODO migrate process definitions.
            Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
            IncidentDbModel dbModel = incidentConverter.apply(historicIncident, processDefinitionKey, processInstanceKey, jobDefinitionKey, flowNodeInstanceKey);
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
    historyService.createHistoricVariableInstanceQuery().list().forEach(historicVariable -> {
      String legacyVariableId = historicVariable.getId();
      if (checkVariableNotMigrated(legacyVariableId)) {
        Long processInstanceKey = findProcessInstanceKey(historicVariable.getProcessInstanceId());
        if (processInstanceKey != null) {
          Long scopeKey = findFlowNodeKey(historicVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
          if (scopeKey != null) {
            LOGGER.info("Migration of legacy variable with id '{}' completed.", legacyVariableId);
            VariableDbModel dbModel = variableConverter.apply(historicVariable, processInstanceKey, scopeKey);
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
        Long processInstanceKey = findProcessInstanceKey(legacyUserTask.getProcessInstanceId());
        if (processInstanceKey != null) {
          LOGGER.info("Migration of legacy user task with id '{}' completed.", legacyUserTaskId);
          Long processDefinitionKey = null; // TODO migrate process definition.
          UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstanceKey);
          userTaskMapper.insert(dbModel);
        } else {
          LOGGER.info("Migration of legacy user task with id '{}' skipped. Process instance not available.", legacyUserTaskId);
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
        Long processInstanceKey = findProcessInstanceKey(legacyFlowNode.getProcessInstanceId());
        if (processInstanceKey != null) {
          LOGGER.info("Migration of legacy flow node with id '{}' completed.", legacyFlowNodeId);
          Long processDefinitionKey = null; // TODO migrate process definition
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

  protected Long findProcessInstanceKey(String processInstanceId) {
    if (processInstanceId == null) return null;

    List<ProcessInstanceDbModel> processInstances = processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b -> b.legacyProcessInstanceId(processInstanceId)));

    if (!processInstances.isEmpty()) {
      return processInstances.get(0).processInstanceKey();
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
