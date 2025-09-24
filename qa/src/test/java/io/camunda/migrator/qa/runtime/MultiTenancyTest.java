/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime;

import io.camunda.client.CamundaClient;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.qa.util.ProcessDefinitionDeployer;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "camunda.process-test.multitenancy-enabled=true" })
@CamundaSpringProcessTest
    //@WithMultiDb
    //@WithSpringProfile("history-level-full")
@Disabled("https://github.com/camunda/camunda-bpm-platform/issues/5414")
class MultiTenancyTest /*extends RuntimeMigrationAbstractTest*/ {

  // Migrator ---------------------------------------

  @Autowired
  protected RuntimeMigrator runtimeMigrator;

  @Autowired
  protected DbClient dbClient;

  @Autowired
  protected ProcessDefinitionDeployer deployer;

  // C7 ---------------------------------------

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected TaskService taskService;

  // C8 ---------------------------------------

  private static final String DEFAULT_USERNAME = "demo";
  private static final String TENANT_ID_1 = "tenant-1";
  private static final String TENANT_ID_2 = "tenant-2";
  @Autowired
  private CamundaClient client;
  @Autowired
  private CamundaProcessTestContext processTestContext;
  private CamundaClient clientForTenant1;

  @BeforeEach
  void setupTenants() {
    // create tenants
    client.newCreateTenantCommand().tenantId(TENANT_ID_1).name(TENANT_ID_1).send().join();
    client.newCreateTenantCommand().tenantId(TENANT_ID_2).name(TENANT_ID_2).send().join();
    // assign the default user to the tenants
    client.newAssignUserToTenantCommand().username(DEFAULT_USERNAME).tenantId(TENANT_ID_1).send().join();
    client.newAssignUserToTenantCommand().username(DEFAULT_USERNAME).tenantId(TENANT_ID_2).send().join();
    // create a client for tenant 1
    clientForTenant1 = processTestContext.createClient(clientBuilder -> clientBuilder.defaultTenantId(TENANT_ID_1));
  }

  @Test
  void createProcessInstance() {
    // given
    clientForTenant1.newDeployResourceCommand()
        .addResourceFromClasspath("io/camunda/migrator/bpmn/c8/simpleProcess.bpmn")
        .send()
        .join();
    // when
    final var processInstance = clientForTenant1.newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .send()
        .join();
    // then
    //    assertThatProcessInstance(processInstance).isCreated();
    Assertions.assertThat(processInstance.getTenantId()).isEqualTo(TENANT_ID_1);
  }

  @Test
  public void shouldMigrateProcessInstanceWithTenant() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn", "my-tenant");
    //    deployer.deployCamunda8Process("simpleProcess.bpmn", "my-tenant");
    clientForTenant1.newDeployResourceCommand()
        .addResourceFromClasspath("io/camunda/migrator/bpmn/c8/simpleProcess.bpmn")
        .send()
        .join();

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess").getId();

    // when
    runtimeMigrator.start();

    // then
    final var processInstance = clientForTenant1.newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .send()
        .join();
    //    assertThatProcessInstanceCountIsEqualTo(1);

    Assertions.assertThat(processInstance.getTenantId()).isEqualTo(TENANT_ID_1);
  }
}