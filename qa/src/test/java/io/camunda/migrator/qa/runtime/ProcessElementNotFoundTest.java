/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.qa.runtime;

import static io.camunda.migrator.impl.logging.RuntimeMigratorLogs.SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR;
import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.FLOW_NODE_NOT_EXISTS_ERROR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.github.netmikey.logunit.api.LogCapturer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ProcessElementNotFoundTest  extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected final LogCapturer logs = LogCapturer.create().captureForType(RuntimeMigrator.class);

  @Test
  public void shouldSkipOnMissingElementInC8Deployment() {
    // given an instance that is currently in an element in the C7 model which does not exist in the C8 model
    deployer.deployProcessInC7AndC8("userTaskProcessWithMissingUserTaskInC8.bpmn");
    var c7Instance = runtimeService.startProcessInstanceByKey("userTaskProcessWithMissingUserTaskInC8Id");

    // when
    runtimeMigrator.start();

    // then
    logs.assertContains(
        String.format(SKIPPING_PROCESS_INSTANCE_VALIDATION_ERROR.replace("{}", "%s"), c7Instance.getId(),
            String.format(FLOW_NODE_NOT_EXISTS_ERROR, "userTaskId")));
    assertThatProcessInstanceCountIsEqualTo(0);
    List<IdKeyDbModel> skippedProcessInstanceIds = dbClient.findSkippedProcessInstances();
    assertThat(skippedProcessInstanceIds.size()).isEqualTo(1);
    assertThat(skippedProcessInstanceIds.getFirst().getC7Id()).isEqualTo(c7Instance.getId());
  }

}
