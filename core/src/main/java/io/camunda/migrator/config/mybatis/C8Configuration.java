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
import io.camunda.db.rdbms.read.service.AuthorizationDbReader;
import io.camunda.db.rdbms.read.service.BatchOperationDbReader;
import io.camunda.db.rdbms.read.service.BatchOperationItemDbReader;
import io.camunda.db.rdbms.read.service.DecisionDefinitionDbReader;
import io.camunda.db.rdbms.read.service.DecisionInstanceDbReader;
import io.camunda.db.rdbms.read.service.DecisionRequirementsDbReader;
import io.camunda.db.rdbms.read.service.FlowNodeInstanceDbReader;
import io.camunda.db.rdbms.read.service.FormDbReader;
import io.camunda.db.rdbms.read.service.GroupDbReader;
import io.camunda.db.rdbms.read.service.IncidentDbReader;
import io.camunda.db.rdbms.read.service.JobDbReader;
import io.camunda.db.rdbms.read.service.MappingRuleDbReader;
import io.camunda.db.rdbms.read.service.ProcessDefinitionDbReader;
import io.camunda.db.rdbms.read.service.ProcessInstanceDbReader;
import io.camunda.db.rdbms.read.service.RoleDbReader;
import io.camunda.db.rdbms.read.service.SequenceFlowDbReader;
import io.camunda.db.rdbms.read.service.TenantDbReader;
import io.camunda.db.rdbms.read.service.UsageMetricsDbReader;
import io.camunda.db.rdbms.read.service.UserDbReader;
import io.camunda.db.rdbms.read.service.UserTaskDbReader;
import io.camunda.db.rdbms.read.service.VariableDbReader;
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
import io.camunda.db.rdbms.sql.MappingRuleMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.PurgeMapper;
import io.camunda.db.rdbms.sql.RoleMapper;
import io.camunda.db.rdbms.sql.SequenceFlowMapper;
import io.camunda.db.rdbms.sql.TenantMapper;
import io.camunda.db.rdbms.sql.UsageMetricMapper;
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
  public MapperFactoryBean<MappingRuleMapper> mappingMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, MappingRuleMapper.class);
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
  public MapperFactoryBean<UsageMetricMapper> usageMetricMapper(final @Qualifier("c8SqlSessionFactory") SqlSessionFactory c8SqlSessionFactory) {
    return createMapperFactoryBean(c8SqlSessionFactory, UsageMetricMapper.class);
  }

  @Bean
  public VariableDbReader variableRdbmsReader(final VariableMapper variableMapper) {
    return new VariableDbReader(variableMapper);
  }

  @Bean
  public AuthorizationDbReader authorizationReader(final AuthorizationMapper authorizationMapper) {
    return new AuthorizationDbReader(authorizationMapper);
  }

  @Bean
  public DecisionDefinitionDbReader decisionDefinitionReader(
      final DecisionDefinitionMapper decisionDefinitionMapper) {
    return new DecisionDefinitionDbReader(decisionDefinitionMapper);
  }

  @Bean
  public DecisionInstanceDbReader decisionInstanceReader(
      final DecisionInstanceMapper decisionInstanceMapper) {
    return new DecisionInstanceDbReader(decisionInstanceMapper);
  }

  @Bean
  public DecisionRequirementsDbReader decisionRequirementsReader(
      final DecisionRequirementsMapper decisionRequirementsMapper) {
    return new DecisionRequirementsDbReader(decisionRequirementsMapper);
  }

  @Bean
  public FlowNodeInstanceDbReader flowNodeInstanceReader(
      final FlowNodeInstanceMapper flowNodeInstanceMapper) {
    return new FlowNodeInstanceDbReader(flowNodeInstanceMapper);
  }

  @Bean
  public GroupDbReader groupReader(final GroupMapper groupMapper) {
    return new GroupDbReader(groupMapper);
  }

  @Bean
  public IncidentDbReader incidentReader(final IncidentMapper incidentMapper) {
    return new IncidentDbReader(incidentMapper);
  }

  @Bean
  public ProcessDefinitionDbReader processDeploymentRdbmsReader(
      final ProcessDefinitionMapper processDefinitionMapper) {
    return new ProcessDefinitionDbReader(processDefinitionMapper);
  }

  @Bean
  public ProcessInstanceDbReader processRdbmsReader(
      final ProcessInstanceMapper processInstanceMapper) {
    return new ProcessInstanceDbReader(processInstanceMapper);
  }

  @Bean
  public TenantDbReader tenantReader(final TenantMapper tenantMapper) {
    return new TenantDbReader(tenantMapper);
  }

  @Bean
  public UserDbReader userRdbmsReader(final UserMapper userTaskMapper) {
    return new UserDbReader(userTaskMapper);
  }

  @Bean
  public RoleDbReader roleRdbmsReader(final RoleMapper roleMapper) {
    return new RoleDbReader(roleMapper);
  }

  @Bean
  public UserTaskDbReader userTaskRdbmsReader(final UserTaskMapper userTaskMapper) {
    return new UserTaskDbReader(userTaskMapper);
  }

  @Bean
  public FormDbReader formRdbmsReader(final FormMapper formMapper) {
    return new FormDbReader(formMapper);
  }

  @Bean
  public MappingRuleDbReader mappingRdbmsReader(final MappingRuleMapper mappingMapper) {
    return new MappingRuleDbReader(mappingMapper);
  }

  @Bean
  public BatchOperationDbReader batchOperationReader(
      final BatchOperationMapper batchOperationMapper) {
    return new BatchOperationDbReader(batchOperationMapper);
  }

  @Bean
  public BatchOperationItemDbReader batchOperationItemReader(
      final BatchOperationMapper batchOperationMapper) {
    return new BatchOperationItemDbReader(batchOperationMapper);
  }

  @Bean
  public JobDbReader jobReader(final JobMapper jobMapper) {
    return new JobDbReader(jobMapper);
  }

  @Bean
  public SequenceFlowDbReader sequenceFlowReader(SequenceFlowMapper sequenceFlowMapper) {
    return new SequenceFlowDbReader(sequenceFlowMapper);
  }

  @Bean
  public UsageMetricsDbReader usageMetricReader(UsageMetricMapper usageMetricMapper) {
    return new UsageMetricsDbReader(usageMetricMapper);
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
      BatchOperationDbReader batchOperationReader,
      JobMapper jobMapper,
      SequenceFlowMapper sequenceFlowMapper,
      UsageMetricMapper usageMetricMapper) {
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
        sequenceFlowMapper,
        usageMetricMapper);
  }

  @Bean
  public RdbmsService rdbmsService(
      final RdbmsWriterFactory rdbmsWriterFactory,
      final VariableDbReader variableReader,
      final AuthorizationDbReader authorizationReader,
      final DecisionDefinitionDbReader decisionDefinitionReader,
      final DecisionInstanceDbReader decisionInstanceReader,
      final DecisionRequirementsDbReader decisionRequirementsReader,
      final FlowNodeInstanceDbReader flowNodeInstanceReader,
      final GroupDbReader groupReader,
      final IncidentDbReader incidentReader,
      final ProcessDefinitionDbReader processDefinitionReader,
      final ProcessInstanceDbReader processInstanceReader,
      final RoleDbReader roleReader,
      final TenantDbReader tenantReader,
      final UserDbReader userReader,
      final UserTaskDbReader userTaskReader,
      final FormDbReader formReader,
      final BatchOperationDbReader batchOperationReader,
      final SequenceFlowDbReader sequenceFlowReader,
      final BatchOperationItemDbReader batchOperationItemReader,
      final JobDbReader jobReader,
      final UsageMetricsDbReader usageMetricsReader) {
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
        null,
        batchOperationReader,
        sequenceFlowReader,
        batchOperationItemReader,
        jobReader,
        usageMetricsReader);
  }

}
