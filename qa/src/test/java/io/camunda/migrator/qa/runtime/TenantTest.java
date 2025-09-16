/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime;

import static io.camunda.migrator.impl.logging.RuntimeMigratorLogs.SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR;
import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.TENANT_ID_ERROR;

import io.camunda.migrator.RuntimeMigrator;
import io.github.netmikey.logunit.api.LogCapturer;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

class TenantTest extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected final LogCapturer LOGS = LogCapturer.create().captureForType(RuntimeMigrator.class);

  @Autowired
  private RuntimeService runtimeService;

  @Test
  public void shouldSkipProcessInstance() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn", "my-tenant");

    String c7ProcessInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess").getId();

    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(0);
    LOGS.assertContains(String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"),
        c7ProcessInstanceId, TENANT_ID_ERROR));
  }


}