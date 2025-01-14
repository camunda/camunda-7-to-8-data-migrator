package io.camunda.migrator;

import io.camunda.db.rdbms.read.domain.UserTaskDbQuery;
import io.camunda.db.rdbms.sql.*;
import io.camunda.db.rdbms.write.domain.*;
import io.camunda.migrator.converter.*;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.IncidentEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.entities.VariableEntity;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;

//@Component
public class CamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMigrator.class);

  private final ProcessInstanceMapper processInstanceMapper;
  private final FlowNodeInstanceMapper flowNodeInstanceMapper;
  private final UserTaskMapper userTaskMapper;
  private final VariableMapper variableMapper;
  private final IncidentMapper incidentMapper;

  private final RuntimeService runtimeService;
  private final HistoryService historyService;

  private final ProcessInstanceConverter processInstanceConverter;
  private final FlowNodeInstanceConverter flowNodeInstanceConverter;
  private final UserTaskConverter userTaskConverter;
  private final VariableConverter variableConverter;
  private final IncidentConverter incidentConverter;

  public CamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      FlowNodeInstanceMapper flowNodeInstanceMapper,
      UserTaskMapper userTaskMapper,
      VariableMapper variableMapper,
      IncidentMapper incidentMapper,

      RuntimeService runtimeService,
      HistoryService historyService,

      ProcessInstanceConverter processInstanceConverter,
      FlowNodeInstanceConverter flowNodeInstanceConverter,
      UserTaskConverter userTaskConverter,
      VariableConverter variableConverter,
      IncidentConverter incidentConverter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.flowNodeInstanceMapper = flowNodeInstanceMapper;
    this.userTaskMapper = userTaskMapper;
    this.variableMapper = variableMapper;
    this.incidentMapper = incidentMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.processInstanceConverter = processInstanceConverter;
    this.flowNodeInstanceConverter = flowNodeInstanceConverter;
    this.userTaskConverter = userTaskConverter;
    this.variableConverter = variableConverter;
    this.incidentConverter = incidentConverter;
  }

  @PostConstruct
  public void migrateAllHistoricProcessInstances() {
    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    runtimeService.startProcessInstanceByKey("simple-process-user-task");
    runtimeService.startProcessInstanceByKey("simple-process-user-task-with-variables");

    addIncidentToProcess("simple-process-user-task", "failedJob");

    migrateProcessInstances();
    migrateFlowNodeInstances();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();
  }

  private void migrateIncidents() {
    LOGGER.info("Migrating Incidents");

    List<IncidentEntity> c8Incidents = incidentMapper.search(null);
    List<Long> migratedKeys = c8Incidents.stream().map(IncidentEntity::incidentKey).toList();

    historyService.createHistoricIncidentQuery().list().forEach(historicIncident -> {
      Long key = convertIdToKey(historicIncident.getId());
      if (!migratedKeys.contains(key)) {
        IncidentDbModel dbModel = incidentConverter.apply(historicIncident);
        incidentMapper.insert(dbModel);
      }
    });


  }

  private void migrateVariables() {
    LOGGER.info("Migrating variables");

    List<VariableEntity> c8Variables = variableMapper.search(null);
    List<Long> migratedKeys = c8Variables.stream().map(VariableEntity::variableKey).toList();

    historyService.createHistoricVariableInstanceQuery().list().forEach(historicVariable -> {
      Long key = convertIdToKey(historicVariable.getId());
      if (!migratedKeys.contains(key)) {
        VariableDbModel dbModel = variableConverter.apply(historicVariable);
        variableMapper.insert(dbModel);
      }
    });
  }

  private void migrateUserTasks() {
    LOGGER.info("Migrating user tasks");

    var dbQuery = UserTaskDbQuery.of(b -> b.page(new SearchQueryPage(0, Integer.MAX_VALUE, new Object[0], new Object[0])));
    List<UserTaskDbModel> userTasks = userTaskMapper.search(dbQuery);
    List<Long> migratedKeys = userTasks.stream().map(UserTaskDbModel::userTaskKey).toList();

    historyService.createHistoricTaskInstanceQuery().list().forEach(historicTask -> {
      Long key = convertIdToKey(historicTask.getId());
      if (!migratedKeys.contains(key)) {
        UserTaskDbModel dbModel = userTaskConverter.apply(historicTask);
        userTaskMapper.insert(dbModel);
      }
    });
  }

  private void migrateFlowNodeInstances() {
    LOGGER.info("Migrating flow node instances ");

    List<FlowNodeInstanceEntity> c8FlowNodeInstances = flowNodeInstanceMapper.search(null);
    List<Long> migratedKeys = c8FlowNodeInstances.stream().map(FlowNodeInstanceEntity::flowNodeInstanceKey).toList();

    historyService.createHistoricActivityInstanceQuery().list().forEach(historicActivity -> {
      Long key = convertActivityInstanceIdToKey(historicActivity.getId());
      if (!migratedKeys.contains(key)) {
        FlowNodeInstanceDbModel dbModel = flowNodeInstanceConverter.apply(historicActivity);
        flowNodeInstanceMapper.insert(dbModel);
      }
    });
  }

  private void migrateProcessInstances() {
    LOGGER.info("Migrating process instances");

    List<ProcessInstanceEntity> c8ProcessInstances = processInstanceMapper.search(null);
    List<Long> migratedKeys = c8ProcessInstances.stream().map(ProcessInstanceEntity::processInstanceKey).toList();

    historyService.createHistoricProcessInstanceQuery().list().forEach(historicProcessInstance -> {
      if (!migratedKeys.contains(convertIdToKey(historicProcessInstance.getId()))) {
        ProcessInstanceDbModel dbModel = processInstanceConverter.apply(historicProcessInstance);
        processInstanceMapper.insert(dbModel);
      }
    });
  }

  private void addIncidentToProcess(String processInstanceId, String incidentType) {
    Execution processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    runtimeService.createIncident(incidentType, processInstance.getId(), "someConfig", "The message of failure");
  }

}
