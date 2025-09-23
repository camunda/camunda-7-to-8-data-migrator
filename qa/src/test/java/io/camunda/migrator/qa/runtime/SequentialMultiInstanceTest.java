/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;

import io.camunda.client.api.search.enums.ElementInstanceState;
import io.camunda.client.api.search.enums.ElementInstanceType;
import io.camunda.client.api.search.response.ElementInstance;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.migrator.RuntimeMigrator;
import io.github.netmikey.logunit.api.LogCapturer;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(locations = "classpath:application-sequential.properties")
class SequentialMultiInstanceTest extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected LogCapturer logs = LogCapturer.create().captureForType(RuntimeMigrator.class);

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  @Test
  public void shouldMigrateSequentialMultiInstanceProcess() {
    // given process state in c7
    deployer.deployProcessInC7AndC8("sequentialMultiInstanceProcess.bpmn");
    var process = runtimeService.startProcessInstanceByKey("multiInstanceProcess", Map.of("namesList", List.of("a", "b", "c")));
    long taskCount = taskService.createTaskQuery().count();
    ensureTrue("Unexpected process state", taskCount == 1);

    // and sequential multi instance activity is at execution 2/3
    String multiInstanceTask = taskService.createTaskQuery().taskDefinitionKey("multiUserTask").singleResult().getId();
    taskService.complete(multiInstanceTask);

    // when
    runtimeMigrator.start();

    // then the instance was migrated
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest()
        .filter(f -> f.processDefinitionId("multiInstanceProcess"))
        .execute()
        .items();
    assertThat(processInstances.size()).isEqualTo(1);

    List<ElementInstance> elements = camundaClient.newElementInstanceSearchRequest()
        .filter(filter -> filter.elementId("multiUserTask"))
        .execute()
        .items();

    assertThat(elements.size()).isEqualTo(1);
    ElementInstance mit = elements.getFirst();
    assertThat(mit.getState()).isEqualTo(ElementInstanceState.ACTIVE);
    assertThat(mit.getType()).isEqualTo(ElementInstanceType.MULTI_INSTANCE_BODY);

    // TODO: check how to assert on variables and further mult i-instance state
  }

}