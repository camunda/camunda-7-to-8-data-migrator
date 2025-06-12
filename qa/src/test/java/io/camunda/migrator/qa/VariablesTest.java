/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;

import io.camunda.process.test.api.CamundaAssert;
import org.junit.jupiter.api.Test;

import io.camunda.migrator.RuntimeMigrator;
import org.springframework.beans.factory.annotation.Autowired;

class VariablesTest extends RuntimeMigrationAbstractTest {

  @Autowired
  protected RuntimeMigrator runtimeMigrator;

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

}
