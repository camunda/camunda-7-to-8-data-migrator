/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.migrator.history.IdKeyMapper;
import java.util.List;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SkipAndRetryProcessInstancesTest extends RuntimeMigrationAbstractTest {

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  @Autowired
  private IdKeyMapper idKeyMapper;

  @Test
  public void shouldSkipMultiInstanceProcessMigrationTest() {
    // given process state in c7
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/multiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("multiInstanceProcess");
    long taskCount = taskService.createTaskQuery().count();
    ensureTrue("Unexpected process state: one task and three parallel tasks should be created", taskCount == 4);

    // when running runtime migration
    runtimeMigrator.migrate();

    // then the instance was not migrated and marked as skipped
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(0, processInstances.size());
    List<String> skippedProcessInstanceIds = idKeyMapper.findSkippedProcessInstanceIds();
    assertEquals(1, skippedProcessInstanceIds.size());
    assertEquals(process.getId(), skippedProcessInstanceIds.getFirst());
  }

  @Test
  public void shouldSkipMultiLevelMultiInstanceProcessMigrationTest() {
    // given process state in c7
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/multiInstanceProcess.bpmn");
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/callMultiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("callMultiInstanceProcess");
    long taskCount = taskService.createTaskQuery().count();
    ensureTrue("Unexpected process state: one task and three parallel tasks should be created", taskCount == 4);

    // when running runtime migration
    runtimeMigrator.migrate();

    // then the instance was not migrated and marked as skipped
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(0, processInstances.size());
    List<String> skippedProcessInstanceIds = idKeyMapper.findSkippedProcessInstanceIds();
    assertEquals(1, skippedProcessInstanceIds.size());
    assertEquals(process.getId(), skippedProcessInstanceIds.getFirst());
  }

  @Test
  public void shouldSkipAgainAProcessInstanceThatWasSkipped() {
    // given skipped process instance
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/multiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("multiInstanceProcess");
    runtimeMigrator.migrate();
    ensureTrue("Unexpected state: one process instance should be skipped", idKeyMapper.findSkippedProcessInstanceIds().size() == 1);

    // when running retrying runtime migration
    runtimeMigrator.setRetryMode(true);
    runtimeMigrator.migrate();

    // then the instance was not migrated and still marked as skipped
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(0, processInstances.size());
    List<String> skippedProcessInstanceIds = idKeyMapper.findSkippedProcessInstanceIds();
    assertEquals(1, skippedProcessInstanceIds.size());
    assertEquals(process.getId(), skippedProcessInstanceIds.getFirst());
  }

  @Test
  public void shouldMigrateFixedProcessInstanceThatWasSkipped() {
    // given skipped process instance
    deployProcessInC7AndC8("multiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("multiInstanceProcess");
    runtimeMigrator.migrate();
    ensureTrue("Unexpected state: one process instance should be skipped", idKeyMapper.findSkippedProcessInstanceIds().size() == 1);

    // and given the process state changed after skipping and is now eligible for migration
    for(Task task : taskService.createTaskQuery().taskDefinitionKey("multiUserTask").list()) {
      taskService.complete(task.getId());
    }
    ensureTrue("Unexpected process state: only one task should be active", taskService.createTaskQuery().count() == 1);

    // when running retrying runtime migration
    runtimeMigrator.setRetryMode(true);
    runtimeMigrator.migrate();

    // then the instance was migrated
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(1, processInstances.size());
    ProcessInstance processInstance = processInstances.getFirst();
    assertEquals(process.getProcessDefinitionKey(), processInstance.getProcessDefinitionId());

    // and the key updated
    assertNotNull(idKeyMapper.findKeyById(process.getId()));
  }

}