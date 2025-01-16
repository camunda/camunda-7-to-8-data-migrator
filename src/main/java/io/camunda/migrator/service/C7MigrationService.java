package io.camunda.migrator.service;

import io.camunda.db.rdbms.sql.*;
import io.camunda.migrator.CamundaMigrator;
import io.camunda.migrator.converter.*;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class C7MigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(C7MigrationService.class);

  // Mappers

  @Autowired
  private ProcessInstanceMapper processInstanceMapper;

  @Autowired
  private FlowNodeInstanceMapper flowNodeInstanceMapper;

  @Autowired
  private UserTaskMapper userTaskMapper;

  @Autowired
  private VariableMapper variableMapper;

  @Autowired
  private IncidentMapper incidentMapper;

  @Autowired
  private ProcessDefinitionMapper processDefinitionMapper;

  // Services

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private HistoryService historyService;

  @Autowired
  private RepositoryService repositoryService;

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

  public void execute() {
    LOGGER.info("Migrating C7 data...");

      var migrator = new CamundaMigrator(
          processInstanceMapper,
          flowNodeInstanceMapper,
          userTaskMapper,
          variableMapper,
          incidentMapper,
          processDefinitionMapper,
          runtimeService,
          historyService,
          repositoryService,
          processInstanceConverter,
          flowNodeConverter,
          userTaskConverter,
          variableConverter,
          incidentConverter,
          processDefinitionConverter
      );

      migrator.migrateAllHistoricProcessInstances();


  }

}
