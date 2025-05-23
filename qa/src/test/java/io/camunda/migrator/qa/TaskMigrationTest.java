/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.byTaskName;

import java.util.stream.Stream;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TaskMigrationTest extends AbstractElementMigrationTest {

  @Autowired
  private RuntimeService runtimeService;

  @Test
  public void migrateUserTaskInstance() {
    // given
    deployProcessInC7AndC8("userTaskProcess.bpmn");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    // when
    runtimeMigrator.migrate();

    // then
    assertThat(byTaskName("UserTaskName")).isCreated().hasElementId("userTaskId");
  }

  @Override
  protected Stream<Arguments> elementScenarios_activeElementPostMigration() {
    return Stream.of(Arguments.of("sendTaskProcess.bpmn", "sendTaskProcessId", "sendTaskId"),
        Arguments.of("receiveTaskProcess.bpmn", "receiveTaskProcessId", "receiveTaskId"),
        Arguments.of("userTaskProcess.bpmn", "userTaskProcessId", "userTaskId"));
  }

  @Override
  protected Stream<Arguments> elementScenarios_completedElementPostMigration() {
    return Stream.of(Arguments.of("manualTaskProcess.bpmn", "manualTaskProcessId", "manualTaskId"));
  }

}
