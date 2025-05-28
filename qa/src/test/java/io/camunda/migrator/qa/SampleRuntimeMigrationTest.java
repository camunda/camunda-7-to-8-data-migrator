/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ElementSelectors.byId;
import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.byTaskName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.process.test.api.CamundaAssert;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class SampleRuntimeMigrationTest extends RuntimeMigrationAbstractTest {

  @Test
  public void simpleProcessMigrationTest() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcess = runtimeService.startProcessInstanceByKey("simpleProcess");
    Task task1 = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task1.getId());
    Task task2 = taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult();
    ensureNotNull("Unexpected process state: userTask2 should exist", task2);
    ensureTrue("Unexpected process state: userTask2 should be 'created'", "created".equalsIgnoreCase(task2.getTaskState()));

    // when running runtime migration
    runtimeMigrator.migrate();

    // then there is one expected process instance
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(1, processInstances.size());
    ProcessInstance processInstance = processInstances.getFirst();
    assertEquals(simpleProcess.getProcessDefinitionKey(), processInstance.getProcessDefinitionId());

    // and the process instance has expected state
    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .isActive()
        .hasActiveElements(byId("userTask2"))
        .hasVariable("legacyId", simpleProcess.getProcessInstanceId());

    // and the user task has expected state
    CamundaAssert.assertThat(byTaskName("User Task 2"))
        .isCreated()
        .hasElementId("userTask2")
        .hasAssignee(null);
  }

  @Test
  public void shouldMigrateMultiLevel(CapturedOutput output) {
    // deploy processes
    deployModels();

    // given
    runtimeService.startProcessInstanceByKey("root");

    // when running runtime migration
    runtimeMigrator.migrate();

    // then
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(3, processInstances.size());

    Matcher matcher = Pattern.compile("Migrator jobs found: 1").matcher(output.getOut());
    assertThat(matcher.results().count()).isEqualTo(3);
  }

  private void deployModels() {
    String rootId = "root";
    String level1Id = "level1";
    String level2Id = "level2";
    // C7
    org.camunda.bpm.model.bpmn.BpmnModelInstance c7rootModel = org.camunda.bpm.model.bpmn.Bpmn.createExecutableProcess(rootId)
        .startEvent("start_1")
        .callActivity("ca_level_1")
        .camundaIn(level1Id, level2Id)
          .calledElement(level1Id)
        .endEvent("end_1").done();
    org.camunda.bpm.model.bpmn.BpmnModelInstance c7level1Model = org.camunda.bpm.model.bpmn.Bpmn.createExecutableProcess(level1Id)
        .startEvent("start_2")
        .callActivity("ca_level_2")
          .calledElement(level2Id)
        .endEvent("end_2").done();
    org.camunda.bpm.model.bpmn.BpmnModelInstance c7level2Model = org.camunda.bpm.model.bpmn.Bpmn.createExecutableProcess(level2Id)
        .startEvent("start_3")
        .userTask("userTask_1")
        .endEvent("end_3").done();

    // C8
    io.camunda.zeebe.model.bpmn.BpmnModelInstance c8rootModel = io.camunda.zeebe.model.bpmn.Bpmn.createExecutableProcess(rootId)
        .startEvent("start_1")
        .zeebeEndExecutionListener("migrator")
        .callActivity("ca_level_1", c -> c.zeebeProcessId(level1Id))
        .endEvent("end_1").done();
    io.camunda.zeebe.model.bpmn.BpmnModelInstance c8level1Model = io.camunda.zeebe.model.bpmn.Bpmn.createExecutableProcess(level1Id)
        .startEvent("start_2")
        .zeebeEndExecutionListener("migrator")
        .callActivity("ca_level_2", c -> c.zeebeProcessId(level2Id))
        .endEvent("end_2").done();
    io.camunda.zeebe.model.bpmn.BpmnModelInstance c8level2Model = io.camunda.zeebe.model.bpmn.Bpmn.createExecutableProcess(level2Id)
        .startEvent("start_3")
        .zeebeEndExecutionListener("migrator")
        .userTask("userTask_1")
        .endEvent("end_3").done();

    repositoryService.createDeployment().addModelInstance(rootId+".bpmn", c7rootModel).deploy();
    repositoryService.createDeployment().addModelInstance(level1Id+".bpmn", c7level1Model).deploy();
    repositoryService.createDeployment().addModelInstance(level2Id+".bpmn", c7level2Model).deploy();

    camundaClient.newDeployResourceCommand().addProcessModel(c8rootModel, rootId+".bpmn").send().join();
    camundaClient.newDeployResourceCommand().addProcessModel(c8level1Model, level1Id+".bpmn").send().join();
    camundaClient.newDeployResourceCommand().addProcessModel(c8level2Model, level2Id+".bpmn").send().join();

  }

}