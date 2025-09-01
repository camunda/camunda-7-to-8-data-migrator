/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.persistence;

import static io.camunda.migrator.impl.logging.RuntimeValidatorLogs.MULTI_INSTANCE_LOOP_CHARACTERISTICS_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;

import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE;
import io.camunda.migrator.qa.runtime.RuntimeMigrationAbstractTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "camunda.migrator.save-skip-reason=true")
public class SaveSkipReasonEnabledIntegrationTest extends RuntimeMigrationAbstractTest {

  @Autowired
  private DbClient dbClient;

  @Autowired
  private IdKeyMapper idKeyMapper;

  @AfterEach
  public void cleanup() {
    dbClient.deleteAllMappings();
  }

  @Test
  public void shouldSaveNullSkipReasonIfSaveSkipReasonIsFalse() {
    // given process state in c7
    deployer.deployProcessInC7AndC8("multiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("multiInstanceProcess");
    long taskCount = taskService.createTaskQuery().count();
    ensureTrue("Unexpected process state: one task and three parallel tasks should be created", taskCount == 4);

    // when
    runtimeMigrator.start();

    // then
    List<IdKeyDbModel> skippedInstances = idKeyMapper.findSkippedByType(TYPE.RUNTIME_PROCESS_INSTANCE, 0, 100);
    assertThat(skippedInstances).hasSize(1);

    IdKeyDbModel savedInstance = skippedInstances.getFirst();
    assertThat(savedInstance.instanceKey()).isNull(); // Skip reason should be null when disabled
    assertThat(savedInstance.skipReason()).isEqualTo(String.format(MULTI_INSTANCE_LOOP_CHARACTERISTICS_ERROR, "multiUserTask")); // No key for skipped instances
  }
}
