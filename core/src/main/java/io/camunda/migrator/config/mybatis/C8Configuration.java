/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config.mybatis;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.config.VendorDatabaseProperties;
import io.camunda.db.rdbms.read.service.AuthorizationReader;
import io.camunda.db.rdbms.read.service.BatchOperationItemReader;
import io.camunda.db.rdbms.read.service.BatchOperationReader;
import io.camunda.db.rdbms.read.service.DecisionDefinitionReader;
import io.camunda.db.rdbms.read.service.DecisionInstanceReader;
import io.camunda.db.rdbms.read.service.DecisionRequirementsReader;
import io.camunda.db.rdbms.read.service.FlowNodeInstanceReader;
import io.camunda.db.rdbms.read.service.FormReader;
import io.camunda.db.rdbms.read.service.GroupReader;
import io.camunda.db.rdbms.read.service.IncidentReader;
import io.camunda.db.rdbms.read.service.MappingReader;
import io.camunda.db.rdbms.read.service.ProcessDefinitionReader;
import io.camunda.db.rdbms.read.service.ProcessInstanceReader;
import io.camunda.db.rdbms.read.service.RoleReader;
import io.camunda.db.rdbms.read.service.SequenceFlowReader;
import io.camunda.db.rdbms.read.service.TenantReader;
import io.camunda.db.rdbms.read.service.UserReader;
import io.camunda.db.rdbms.read.service.UserTaskReader;
import io.camunda.db.rdbms.read.service.VariableReader;
import io.camunda.db.rdbms.sql.AuthorizationMapper;
import io.camunda.db.rdbms.sql.BatchOperationMapper;
import io.camunda.db.rdbms.sql.DecisionDefinitionMapper;
import io.camunda.db.rdbms.sql.DecisionInstanceMapper;
import io.camunda.db.rdbms.sql.DecisionRequirementsMapper;
import io.camunda.db.rdbms.sql.ExporterPositionMapper;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.FormMapper;
import io.camunda.db.rdbms.sql.GroupMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.JobMapper;
import io.camunda.db.rdbms.sql.MappingMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.PurgeMapper;
import io.camunda.db.rdbms.sql.RoleMapper;
import io.camunda.db.rdbms.sql.SequenceFlowMapper;
import io.camunda.db.rdbms.sql.TenantMapper;
import io.camunda.db.rdbms.sql.UserMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.RdbmsWriterFactory;
import io.camunda.migrator.config.C8DataSourceConfigured;
import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.spring.client.metrics.MetricsRecorder;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(C8DataSourceConfigured.class)
public class C8Configuration extends AbstractConfiguration {

  @Autowired
  @Qualifier("c8DataSource")
  protected DataSource dataSource;

  @Bean
  @ConditionalOnProperty(prefix = MigratorProperties.PREFIX + ".c8.data-source", name = "auto-ddl", havingValue = "true")
  public MultiTenantSpringLiquibase createRdbmsExporterSchema() {
    return createSchema(dataSource, configProperties.getC8().getDataSource().getTablePrefix(),
        "db/changelog/rdbms-exporter/changelog-master.xml");
  }

  @Bean
  public DbVendorProvider dbVendorProvider() {
    String vendor = configProperties.getC8().getDataSource().getVendor();
    return new DbVendorProvider(vendor);
  }

  @Bean
  public VendorDatabaseProperties vendorDatabaseProperties(DbVendorProvider dbVendorProvider) throws Exception {
    String c8DbVendor = dbVendorProvider.getDatabaseId(dataSource);
    String c8File = "db/vendor-properties/" + c8DbVendor + ".properties";

    return new VendorDatabaseProperties(loadPropertiesFile(c8DbVendor, c8File));
  }

  @Bean
  public SqlSessionFactory c8SqlSessionFactory(VendorDatabaseProperties vendorDatabaseProperties, DbVendorProvider dbVendorProvider) throws Exception {
    String tablePrefix = this.configProperties.getC8().getDataSource().getTablePrefix();
    Properties properties = vendorDatabaseProperties.properties();
    return createSqlSessionFactory(dataSource, dbVendorProvider, properties, tablePrefix);
  }

  @Bean
  public MapperFactoryBean<JobMapper> jobMapper(@Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, JobMapper.class);
  }

  @Bean
  public MapperFactoryBean<BatchOperationMapper> batchOperationMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, BatchOperationMapper.class);
  }

  @Bean
  public MapperFactoryBean<AuthorizationMapper> authorizationMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, AuthorizationMapper.class);
  }

  @Bean
  public MapperFactoryBean<DecisionDefinitionMapper> decisionDefinitionMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, DecisionDefinitionMapper.class);
  }

  @Bean
  public MapperFactoryBean<DecisionInstanceMapper> decisionInstanceMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, DecisionInstanceMapper.class);
  }

  @Bean
  public MapperFactoryBean<DecisionRequirementsMapper> decisionRequirementsMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, DecisionRequirementsMapper.class);
  }

  @Bean
  public MapperFactoryBean<FlowNodeInstanceMapper> flowNodeInstanceMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, FlowNodeInstanceMapper.class);
  }

  @Bean
  public MapperFactoryBean<GroupMapper> groupInstanceMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, GroupMapper.class);
  }

  @Bean
  public MapperFactoryBean<IncidentMapper> incidentMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, IncidentMapper.class);
  }

  @Bean
  public MapperFactoryBean<ProcessInstanceMapper> processInstanceMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, ProcessInstanceMapper.class);
  }

  @Bean
  public MapperFactoryBean<ProcessDefinitionMapper> processDeploymentMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, ProcessDefinitionMapper.class);
  }

  @Bean
  public MapperFactoryBean<TenantMapper> tenantMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, TenantMapper.class);
  }

  @Bean
  public MapperFactoryBean<VariableMapper> variableMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, VariableMapper.class);
  }

  @Bean
  public MapperFactoryBean<RoleMapper> roleMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, RoleMapper.class);
  }

  @Bean
  public MapperFactoryBean<UserMapper> userMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, UserMapper.class);
  }

  @Bean
  public MapperFactoryBean<UserTaskMapper> userTaskMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, UserTaskMapper.class);
  }

  @Bean
  public MapperFactoryBean<FormMapper> formMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, FormMapper.class);
  }

  @Bean
  public MapperFactoryBean<MappingMapper> mappingMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, MappingMapper.class);
  }

  @Bean
  public MapperFactoryBean<ExporterPositionMapper> exporterPosition(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, ExporterPositionMapper.class);
  }

  @Bean
  public MapperFactoryBean<PurgeMapper> purgeMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, PurgeMapper.class);
  }

  @Bean
  public MapperFactoryBean<SequenceFlowMapper> sequenceFlowMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, SequenceFlowMapper.class);
  }

  @Bean
  public MapperFactoryBean<MetricsRecorder> metricsRecorder(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, MetricsRecorder.class);
  }

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
  public BatchOperationReader batchOperationReader(
      final BatchOperationMapper batchOperationMapper) {
    return new BatchOperationReader(batchOperationMapper);
  }

  @Bean
  public BatchOperationItemReader batchOperationItemReader(
      final BatchOperationMapper batchOperationMapper) {
    return new BatchOperationItemReader(batchOperationMapper);
  }

  @Bean
  public SequenceFlowReader sequenceFlowReader(SequenceFlowMapper sequenceFlowMapper) {
    return new SequenceFlowReader(sequenceFlowMapper);
  }

  @Bean
  public RdbmsWriterFactory rdbmsWriterFactory(
      @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory,
      ExporterPositionMapper exporterPositionMapper,
      VendorDatabaseProperties dbVendorProvider,
      DecisionInstanceMapper decisionInstanceMapper,
      FlowNodeInstanceMapper flowNodeInstanceMapper,
      IncidentMapper incidentMapper,
      ProcessInstanceMapper processInstanceMapper,
      PurgeMapper purgeMapper,
      UserTaskMapper userTaskMapper,
      VariableMapper variableMapper,
      BatchOperationReader batchOperationReader,
      JobMapper jobMapper,
      SequenceFlowMapper sequenceFlowMapper) {
    return new RdbmsWriterFactory(
        c8SqlSessionFactory,
        exporterPositionMapper,
        dbVendorProvider,
        decisionInstanceMapper,
        flowNodeInstanceMapper,
        incidentMapper,
        processInstanceMapper,
        purgeMapper,
        userTaskMapper,
        variableMapper,
        null,
        batchOperationReader,
        jobMapper,
        sequenceFlowMapper);
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
      final MappingReader mappingReader,
      final BatchOperationReader batchOperationReader,
      final SequenceFlowReader sequenceFlowReader,
      final BatchOperationItemReader batchOperationItemReader) {
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
        mappingReader,
        batchOperationReader,
        sequenceFlowReader,
        batchOperationItemReader);
  }

}
