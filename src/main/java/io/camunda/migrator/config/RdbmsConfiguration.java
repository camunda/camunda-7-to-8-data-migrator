/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.read.service.*;
import io.camunda.db.rdbms.sql.*;
import io.camunda.db.rdbms.write.RdbmsWriterFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
public class RdbmsConfiguration {

  @Bean
  public VariableReader variableRdbmsReader(final VariableMapper variableMapper) {
    return new VariableReader(variableMapper);
  }

  @Bean
  public AuthorizationReader authorizationReader(final AuthorizationMapper authorizationMapper) {
    return new AuthorizationReader(authorizationMapper);
  }

  @Bean
  public DecisionDefinitionReader decisionDefinitionReader(
      final DecisionDefinitionMapper decisionDefinitionMapper) {
    return new DecisionDefinitionReader(decisionDefinitionMapper);
  }

  @Bean
  public DecisionInstanceReader decisionInstanceReader(
      final DecisionInstanceMapper decisionInstanceMapper) {
    return new DecisionInstanceReader(decisionInstanceMapper);
  }

  @Bean
  public DecisionRequirementsReader decisionRequirementsReader(
      final DecisionRequirementsMapper decisionRequirementsMapper) {
    return new DecisionRequirementsReader(decisionRequirementsMapper);
  }

  @Bean
  public FlowNodeInstanceReader flowNodeInstanceReader(
      final FlowNodeInstanceMapper flowNodeInstanceMapper) {
    return new FlowNodeInstanceReader(flowNodeInstanceMapper);
  }

  @Bean
  public GroupReader groupReader(final GroupMapper groupMapper) {
    return new GroupReader(groupMapper);
  }

  @Bean
  public IncidentReader incidentReader(final IncidentMapper incidentMapper) {
    return new IncidentReader(incidentMapper);
  }

  @Bean
  public ProcessDefinitionReader processDeploymentRdbmsReader(
      final ProcessDefinitionMapper processDefinitionMapper) {
    return new ProcessDefinitionReader(processDefinitionMapper);
  }

  @Bean
  public ProcessInstanceReader processRdbmsReader(
      final ProcessInstanceMapper processInstanceMapper) {
    return new ProcessInstanceReader(processInstanceMapper);
  }

  @Bean
  public TenantReader tenantReader(final TenantMapper tenantMapper) {
    return new TenantReader(tenantMapper);
  }

  @Bean
  public UserReader userRdbmsReader(final UserMapper userTaskMapper) {
    return new UserReader(userTaskMapper);
  }

  @Bean
  public RoleReader roleRdbmsReader(final RoleMapper roleMapper) {
    return new RoleReader(roleMapper);
  }

  @Bean
  public UserTaskReader userTaskRdbmsReader(final UserTaskMapper userTaskMapper) {
    return new UserTaskReader(userTaskMapper);
  }

  @Bean
  public FormReader formRdbmsReader(final FormMapper formMapper) {
    return new FormReader(formMapper);
  }

  @Bean
  public MappingReader mappingRdbmsReader(final MappingMapper mappingMapper) {
    return new MappingReader(mappingMapper);
  }

  @Bean
  public RdbmsWriterFactory rdbmsWriterFactory(
      final SqlSessionFactory sqlSessionFactory,
      final ExporterPositionMapper exporterPositionMapper,
      final PurgeMapper purgeMapper) {
    return new RdbmsWriterFactory(sqlSessionFactory, exporterPositionMapper, purgeMapper);
  }

  @Bean
  public RdbmsService rdbmsService(
      final RdbmsWriterFactory rdbmsWriterFactory,
      final VariableReader variableReader,
      final AuthorizationReader authorizationReader,
      final DecisionDefinitionReader decisionDefinitionReader,
      final DecisionInstanceReader decisionInstanceReader,
      final DecisionRequirementsReader decisionRequirementsReader,
      final FlowNodeInstanceReader flowNodeInstanceReader,
      final GroupReader groupReader,
      final IncidentReader incidentReader,
      final ProcessDefinitionReader processDefinitionReader,
      final ProcessInstanceReader processInstanceReader,
      final RoleReader roleReader,
      final TenantReader tenantReader,
      final UserReader userReader,
      final UserTaskReader userTaskReader,
      final FormReader formReader,
      final MappingReader mappingReader) {
    return new RdbmsService(
        rdbmsWriterFactory,
        authorizationReader,
        decisionDefinitionReader,
        decisionInstanceReader,
        decisionRequirementsReader,
        flowNodeInstanceReader,
        groupReader,
        incidentReader,
        processDefinitionReader,
        processInstanceReader,
        variableReader,
        roleReader,
        tenantReader,
        userReader,
        userTaskReader,
        formReader,
        mappingReader);
  }
}
