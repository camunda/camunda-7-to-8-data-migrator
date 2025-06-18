/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.byTaskName;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import io.camunda.migrator.qa.variables.JsonSerializable;
import io.camunda.migrator.qa.variables.XmlSerializable;
import io.camunda.process.test.api.CamundaAssert;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VariablesTest extends RuntimeMigrationAbstractTest {

  public static final String SUB_PROCESS = "subProcess";
  public static final String PARALLEL = "parallel";

  @Autowired
  private ObjectMapper objectMapper;
  @Test
  public void shouldSetVariableWithNullValue() {
    // given
    deployProcessInC7AndC8("simpleProcess.bpmn");


    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "myVariable", null);

    // when
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("myVariable", null);
  }
  
  @Test
  public void shouldSetStringVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "stringVar", "myStringVar");

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("stringVar", "myStringVar");
  }

  @Test
  public void shouldSetBooleanVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "booleanVar", true);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("booleanVar", true);
  }

  @Test
  public void shouldSetIntegerVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "integerVar", 1234);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("integerVar", 1234);
  }

  @Test
  public void shouldSetC7DoubleAsC8FloatVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "floatVar", 1.5f);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("floatVar", 1.5);
    List<Variable> c8vars = camundaClient.newVariableSearchRequest()
        .filter(f -> f.name("floatVar"))
        .send().join().items();
  }

  @Test
  public void shouldSetDoubleVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "doubleVar", 1.5d);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("doubleVar", 1.5d);
  }

  @Test
  public void shouldSetShortVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "shortVar", (short)1);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("shortVar", (short)1);
  }

  @Test
  public void shouldSetByteVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "byteVar", (byte)1);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("byteVar", (byte)1);
  }

  @Test
  public void shouldSetCharVariable() {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "charVar", (char)1);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("charVar", (char)1);
  }

  @Test
  public void shouldSetXmlVariable() throws JsonProcessingException {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

    XmlSerializable foo = new XmlSerializable("a String", 42, true);
    String xml = objectMapper.writeValueAsString(foo);
    runtimeService.setVariable(simpleProcessInstance.getId(), "var", xml);
    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("var", "{\"stringProperty\":\"a String\",\"intProperty\":42,\"booleanProperty\":true}");
  }

  @Test
  public void shouldSetJsonVariable() throws JsonProcessingException {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

    JsonSerializable foo = new JsonSerializable("a String", 42, true);
    String json = objectMapper.writeValueAsString(foo);
    runtimeService.setVariable(simpleProcessInstance.getId(), "var", json);
    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("var", "{\"stringProperty\":\"a String\",\"intProperty\":42,\"booleanProperty\":true}");
  }

  @Test
  public void shouldSetGlobalVariable() {
    // deploy processes
    deployModels();
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    // given
    runtimeService.startProcessInstanceByKey(SUB_PROCESS, vars);
    Task currentTask = taskService.createTaskQuery().singleResult();
    runtimeService.setVariable(currentTask.getExecutionId(), "localVariable", "local value");

    // when running runtime migration
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId(SUB_PROCESS))
        .hasVariableNames("variable1","variable2", "localVariable");
  }

  @Test
  public void shouldSetLocalVariable() {
    // deploy processes
    deployModels2();
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");

    // given
    runtimeService.startProcessInstanceByKey(PARALLEL, vars);
    Task currentTask = taskService.createTaskQuery().taskDefinitionKey("userTask_1").singleResult();
    runtimeService.setVariable(currentTask.getExecutionId(), "localVariable", "local value");

    // when running runtime migration
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId(PARALLEL))
        .hasVariableNames("variable1");

    // TODO assert local variable when this ticket is completed https://github.com/camunda/camunda/issues/32648
    List<Variable> c8vars = camundaClient.newVariableSearchRequest()
        .filter(f -> f.name("localVariable"))
        .send().join().items();

    assertThat(c8vars.size()).isEqualTo(1);
    var c8Var = c8vars.get(0);
    assertThat(c8Var.getValue()).isEqualTo("local value");
    // TODO
//    assertThat(c8Var.getScopeKey()).isEqualTo(tasks.get(0).getUserTaskKey());
  }
  @Test
  @Disabled
  public void shouldSetVariableIntoSubprocess() {
    // deploy processes
    deployModels();
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    // given
    runtimeService.startProcessInstanceByKey(SUB_PROCESS, vars);
    Task currentTask = taskService.createTaskQuery().singleResult();
    runtimeService.setVariableLocal(currentTask.getExecutionId(), "localVariable", "local value");

    // when running runtime migration
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId(SUB_PROCESS))
        .hasVariableNames("variable1","variable2");

    // TODO assert local variable when this ticket is completed https://github.com/camunda/camunda/issues/32648
    List<Variable> c8vars = camundaClient.newVariableSearchRequest()
        .filter(f -> f.name("localVariable"))
        .send().join().items();

    assertThat(c8vars.size()).isEqualTo(1);
    var c8Var = c8vars.get(0);
    assertThat(c8Var.getValue()).isEqualTo("local value");
    assertThat(c8Var.getScopeKey()).isEqualTo("userTask_1");
  }

  @Test
  public void shouldSetTaskVariable() {
    // deploy processes
    deployModels();
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    // given
    runtimeService.startProcessInstanceByKey(SUB_PROCESS, vars);
    Task currentTask = taskService.createTaskQuery().singleResult();
    taskService.setVariable(currentTask.getId(), "taskVariable", "value");

    // when running runtime migration
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId(SUB_PROCESS))
        .hasVariableNames("variable1","variable2");

    List<Variable> c8vars = camundaClient.newVariableSearchRequest()
                .filter(f -> f.name("taskVariable"))
        .send().join().items();
    assertThat(c8vars.size()).isEqualTo(1);
    assertThat(c8vars.get(0).getValue().contains("value")).isTrue();
    // TODO scope
  }



  private void deployModels() {
    String process = SUB_PROCESS;
    // C7
    var c7Model = org.camunda.bpm.model.bpmn.Bpmn.createExecutableProcess(process)
        .startEvent("start_1")
        .subProcess("sub_1")
           .embeddedSubProcess()
           .startEvent("start_2")
           .userTask("userTask_1")
           .endEvent("end_2")
           .subProcessDone()
        .endEvent("end_1").done();

    // C8
    var c8Model = io.camunda.zeebe.model.bpmn.Bpmn.createExecutableProcess(process)
        .startEvent("start_1")
        .zeebeEndExecutionListener("migrator")
        .subProcess()
           .embeddedSubProcess()
           .startEvent("start_2")
           .userTask("userTask_1")
           .endEvent("end_2")
           .subProcessDone()
        .endEvent("end_1").done();


    repositoryService.createDeployment().addModelInstance(process+".bpmn", c7Model).deploy();

    camundaClient.newDeployResourceCommand().addProcessModel(c8Model, process+".bpmn").send().join();
  }
  private void deployModels2() {
    String process = PARALLEL;
    // C7
    var c7Model = org.camunda.bpm.model.bpmn.Bpmn.createExecutableProcess(process)
        .startEvent("start_1")
        .parallelGateway("fork")
            .userTask("userTask_1")
        .parallelGateway("join")
        .endEvent("end_1")
        .moveToNode("fork")
            .userTask("userTask_2")
            .connectTo("join")
        .done();

    // C8
    var c8Model = io.camunda.zeebe.model.bpmn.Bpmn.createExecutableProcess(process)
        .startEvent("start_1")
        .zeebeEndExecutionListener("migrator")
        .parallelGateway("fork")
            .userTask("userTask_1")
        .parallelGateway("join")
        .endEvent("end_1")
        .moveToNode("fork")
            .userTask("userTask_2")
        .connectTo("join")
        .done();


    repositoryService.createDeployment().addModelInstance(process+".bpmn", c7Model).deploy();

    camundaClient.newDeployResourceCommand().addProcessModel(c8Model, process+".bpmn").send().join();
  }
}
