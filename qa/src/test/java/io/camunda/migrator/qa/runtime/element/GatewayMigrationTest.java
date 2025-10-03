/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.runtime.element;

import static io.camunda.migrator.constants.MigratorConstants.LEGACY_ID_VAR_NAME;
import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static io.camunda.process.test.api.assertions.ElementSelectors.byId;
import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.byTaskName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.qa.runtime.RuntimeMigrationAbstractTest;
import java.util.Map;
import org.awaitility.Awaitility;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GatewayMigrationTest extends RuntimeMigrationAbstractTest {

  @Autowired
  private RuntimeService runtimeService;

  @Test
  public void migrateEventBasedActivityInstance() {
    // given
    deployer.deployProcessInC7AndC8("eventGateway.bpmn");

    // For C8 correlation variables are required
    Map<String, Object> variables = Variables.createVariables()
        .putValue("catchEvent1CorrelationVariable", "12345")
        .putValue("catchEvent2CorrelationVariable", 99.9);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("eventGatewayProcessId", variables);

    // when
    runtimeMigrator.start();

    // then
    assertThat(byProcessId("eventGatewayProcessId")).isActive()
        .hasActiveElements(byId("eventGatewayElementId"))
        .hasVariables(Map.of(
            LEGACY_ID_VAR_NAME, instance.getProcessInstanceId(),
            "catchEvent1CorrelationVariable", "12345",
            "catchEvent2CorrelationVariable", 99.9
        ));
    
  }
  
  @Test
  public void migrateParallelGatewayActivityInstance() {
    // while the parallel gateway has no natural wait state, we can test that the tokens are in a consistent state
    // between the Splitting Parallel Gateway & the Merging Parallel Gateway after migrating to C8
    // given
    deployer.deployProcessInC7AndC8("parallelGateway.bpmn");

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("ParallelGatewayProcess");

    // when
    runtimeMigrator.start();

    // then
    long c8ProcessInstanceKey = dbClient.findC8KeyByC7IdAndType(instance.getProcessInstanceId(), IdKeyMapper.TYPE.RUNTIME_PROCESS_INSTANCE);

    assertThat(byProcessId("ParallelGatewayProcess")).isActive()
        .hasActiveElementsExactly(byId("usertaskActivity"))
        .hasCompletedElement(byId("noOpActivity"),1)
        .hasCompletedElement(byId("mergingGatewayActivity"), 0)
        .hasVariable(LEGACY_ID_VAR_NAME, instance.getProcessInstanceId());


    // when - complete the task in C8
    var userTasks = camundaClient.newUserTaskSearchRequest()
        .filter(f -> f.processInstanceKey(c8ProcessInstanceKey))
        .execute().items();
    assertEquals(1, userTasks.size());

    var userTask = userTasks.getFirst();
    camundaClient.newCompleteUserTaskCommand(userTask.getUserTaskKey()).execute();

    // then - verify the process instance is completed
    Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(10))
        .pollInterval(java.time.Duration.ofMillis(200))
        .untilAsserted(() -> {
          var processInstances = camundaClient.newProcessInstanceSearchRequest()
              .filter(f -> f.processInstanceKey(c8ProcessInstanceKey))
              .execute().items();
          assertEquals(1, processInstances.size(), "Process instance should still exist but be completed");

          assertEquals(ProcessInstanceState.COMPLETED, processInstances.getFirst().getState(), "Process instance should be in COMPLETED state");
          assertNotNull(processInstances.getFirst().getEndDate(), "Process instance should have an end date");
        });
  }
}
