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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.github.netmikey.logunit.api.LogCapturer;
import java.util.List;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProcessDefinitionNotFoundTest extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected final LogCapturer logs = LogCapturer.create().captureForType(RuntimeMigrator.class);

  @Test
  public void shouldSkipOnMissingC8Deployment() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn");
    var c7Instance = runtimeService.startProcessInstanceByKey("simpleProcess");

    // when
    runtimeMigrator.start();

    // then
    logs.assertContains(
        String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"), c7Instance.getId(),
            String.format(NO_C8_DEPLOYMENT_ERROR, "simpleProcess", c7Instance.getId())));
    assertThatProcessInstanceCountIsEqualTo(0);
    List<IdKeyDbModel> skippedProcessInstanceIds = dbClient.findSkippedProcessInstances();
    assertThat(skippedProcessInstanceIds.size()).isEqualTo(1);
    assertThat(skippedProcessInstanceIds.getFirst().getId()).isEqualTo(c7Instance.getId());
  }

  @Test
  public void shouldSkipNotExistingProcessIdempotently() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn");
    deployer.deployProcessInC7AndC8("userTaskProcess.bpmn");

    var c7Instance = runtimeService.startProcessInstanceByKey("simpleProcess");
    ClockUtil.offset(50_000L);
    runtimeService.startProcessInstanceByKey("userTaskProcessId");

    // when
    runtimeMigrator.start();

    // then
    String missingDefinitionLog = String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"),
        c7Instance.getId(), String.format(NO_C8_DEPLOYMENT_ERROR, "simpleProcess", c7Instance.getId()));
    long logCountAfterFirstRun = logs.getEvents().stream()
        .filter(event -> event.getMessage().contains(missingDefinitionLog))
        .count();
    assertThat(logCountAfterFirstRun).isEqualTo(1);

    // when
    runtimeMigrator.start();

    // then no additional log entry is created
    long logCountAfterSecondRun = logs.getEvents().stream()
        .filter(event -> event.getMessage().contains(missingDefinitionLog))
        .count();
    assertThat(logCountAfterSecondRun).isEqualTo(1);
  }

}
