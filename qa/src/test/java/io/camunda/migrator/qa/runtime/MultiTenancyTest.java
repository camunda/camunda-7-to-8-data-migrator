/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime;

import static io.camunda.migrator.impl.logging.RuntimeMigratorLogs.SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR;
import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.NO_C8_DEPLOYMENT_ERROR;
import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.NO_C8_TENANT_DEPLOYMENT_ERROR;
import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.TENANT_ID_ERROR;
import static io.camunda.process.test.api.CamundaAssert.assertThatProcessInstance;
import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.github.netmikey.logunit.api.LogCapturer;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "camunda.process-test.multi-tenancy-enabled=true",
    "camunda.migrator.tenantIds=tenant-1,tenant-2" })
class MultiTenancyTest extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected final LogCapturer logs = LogCapturer.create().captureForType(RuntimeMigrator.class);
  // C8 ---------------------------------------

  protected static final String DEFAULT_USERNAME = "demo";
  protected static final String TENANT_ID_1 = "tenant-1";
  protected static final String TENANT_ID_2 = "tenant-2";
  @Autowired
  protected CamundaClient client;
  @Autowired
  protected CamundaProcessTestContext processTestContext;
  protected CamundaClient clientForTenant1;

  @BeforeEach
  void setupTenants() {
    // create tenants
    client.newCreateTenantCommand().tenantId(TENANT_ID_1).name(TENANT_ID_1).send().join();
    client.newCreateTenantCommand().tenantId(TENANT_ID_2).name(TENANT_ID_2).send().join();
    // assign the default user to the tenants
    client.newAssignUserToTenantCommand().username(DEFAULT_USERNAME).tenantId(TENANT_ID_1).send().join();
    client.newAssignUserToTenantCommand().username(DEFAULT_USERNAME).tenantId(TENANT_ID_2).send().join();
  }

  @Test
  @Disabled
  public void shouldMigrateProcessInstanceWithTenant() {
    // given
    deployer.deployProcessInC7AndC8("simpleProcess.bpmn", TENANT_ID_1);

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess",
        Variables.putValue("myVar", 1234)).getId();

    // when
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId("simpleProcess")).isActive();
    var c8ProcessInstanceTenant = client.newProcessInstanceSearchRequest()
        .filter(f -> f.processDefinitionId("simpleProcess"))
        .send()
        .join()
        .items()
        .getFirst()
        .getTenantId();
    var c8VariableTenant = client.newVariableSearchRequest()
        .filter(f -> f.name("myVar"))
        .send()
        .join()
        .items()
        .getFirst()
        .getTenantId();
    assertThat(c8ProcessInstanceTenant).isEqualTo(TENANT_ID_1);
    assertThat(c8VariableTenant).isEqualTo(TENANT_ID_1);
  }

  @Test
  public void shouldSkipProcessInstanceWhenProcessDefinitionHasNoTenant() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn", TENANT_ID_1);
    deployer.deployCamunda8Process("simpleProcess.bpmn");

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess").getId();

    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(0);
    logs.assertContains(
        String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"), c7ProcessInstanceId,
            String.format(NO_C8_TENANT_DEPLOYMENT_ERROR, "simpleProcess", TENANT_ID_1, c7ProcessInstanceId)));
  }

  @Test
  public void shouldSkipProcessInstanceWhenProcessDefinitionHasDifferentTenant() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn", TENANT_ID_1);
    deployer.deployCamunda8Process("simpleProcess.bpmn", TENANT_ID_2);

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess").getId();

    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(0);
    logs.assertContains(
        String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"), c7ProcessInstanceId,
            String.format(NO_C8_TENANT_DEPLOYMENT_ERROR, "simpleProcess", TENANT_ID_1, c7ProcessInstanceId)));
  }

}