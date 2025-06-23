/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.process.test.api.CamundaAssert;
import java.util.List;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.spin.plugin.variable.SpinValues;
import org.camunda.spin.plugin.variable.value.JsonValue;
import org.camunda.spin.plugin.variable.value.XmlValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SpinVariablesTest extends RuntimeMigrationAbstractTest {

  @Autowired
  ProcessEngineConfigurationImpl pec;

  @Test
  @Disabled
  public void shouldSetSpinJsonVariable() throws JsonProcessingException {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

    String json = "{\"name\" : \"jonny\","
        + "\"address\" : {"
        + "\"street\" : \"12 High Street\","
        + "\"post code\" : 1234"
        + "}"
        + "}";
        JsonValue jsonValue = SpinValues.jsonValue(json).create();

    runtimeService.setVariable(simpleProcessInstance.getId(), "var", jsonValue);
    TypedValue c7var = runtimeService.getVariableTyped(simpleProcessInstance.getId(), "var", false);

    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("var", c7var.getValue().toString());
  }

  @Test
  @Disabled
  public void shouldSetXmlVariable() throws JsonProcessingException {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given process state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

    String xml = "<customer xmlns=\"http:\\/\\/camunda.org/example\" name=\"Jonny\">"
        + "<address>"
        + "<street>12 High Street</street>"
        + "<postCode>1234</postCode>"
        + "</address>"
        + "</customer>";
    XmlValue xmlValue = SpinValues.xmlValue(xml).create();
    runtimeService.setVariable(simpleProcessInstance.getId(), "var", xml);
    // when running runtime migration
    runtimeMigrator.start();

    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("var", xml);
  }
}
