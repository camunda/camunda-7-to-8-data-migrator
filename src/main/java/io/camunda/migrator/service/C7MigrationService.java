package io.camunda.migrator.service;

import io.camunda.db.rdbms.sql.*;
import io.camunda.migrator.CamundaMigrator;
import io.camunda.migrator.converter.*;
import org.camunda.bpm.engine.HistoryService;
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

  // Services

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private HistoryService historyService;

  // Converters

  @Autowired
  private ProcessInstanceConverter processInstanceConverter;

  @Autowired
  private FlowNodeInstanceConverter flowNodeInstanceConverter;

  @Autowired
  private UserTaskConverter userTaskConverter;

  @Autowired
  private VariableConverter variableConverter;

  @Autowired
  private IncidentConverter incidentConverter;

  public void execute() {
    LOGGER.info("Migrating C7 data...");

      var migrator = new CamundaMigrator(
          processInstanceMapper,
          flowNodeInstanceMapper,
          userTaskMapper,
          variableMapper,
          incidentMapper,
          runtimeService,
          historyService,
          processInstanceConverter,
          flowNodeInstanceConverter,
          userTaskConverter,
          variableConverter,
          incidentConverter
      );

      migrator.migrateAllHistoricProcessInstances();


  }

}