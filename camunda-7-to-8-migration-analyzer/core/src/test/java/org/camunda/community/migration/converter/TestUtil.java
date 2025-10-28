/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class TestUtil {

  public static Map<String, Object> mockVariables(String... names) {
    Map<String, Object> variables = new HashMap<>();
    for (String name : names) {
      variables.put(name, RandomStringUtils.random(20));
    }
    return variables;
  }

  public static Map<String, Object> mockVariables(int count) {
    String[] names = new String[count];
    for (int i = 0; i < count; i++) {
      names[i] = RandomStringUtils.random(5);
    }
    return mockVariables(names);
  }

  public static BpmnModelInstance loadAndConvert(String bpmnFile) {
    BpmnModelInstance modelInstance = loadModelInstance(bpmnFile);
    DiagramConverter converter = DiagramConverterFactory.getInstance().get();
    ConverterProperties properties = ConverterPropertiesFactory.getInstance().get();
    converter.convert(modelInstance, properties);
    return modelInstance;
  }

  public static BpmnModelInstance loadAndConvert(String bpmnFile, String targetVersion) {
    BpmnModelInstance modelInstance = loadModelInstance(bpmnFile);
    DiagramConverter converter = DiagramConverterFactory.getInstance().get();
    DefaultConverterProperties properties = new DefaultConverterProperties();
    properties.setPlatformVersion(targetVersion);
    converter.convert(modelInstance, ConverterPropertiesFactory.getInstance().merge(properties));
    return modelInstance;
  }

  public static BpmnModelInstance loadAndConvert(String bpmnFile, ConverterProperties properties) {
    BpmnModelInstance modelInstance = loadModelInstance(bpmnFile);
    DiagramConverter converter = DiagramConverterFactory.getInstance().get();
    converter.convert(modelInstance, properties);
    return modelInstance;
  }

  public static DiagramCheckResult loadAndCheck(String bpmnFile) {
    return loadAndCheck(bpmnFile, ConverterPropertiesFactory.getInstance().get());
  }

  public static DiagramCheckResult loadAndCheckAgainstVersion(
      String bpmnFile, String targetVersion) {
    DefaultConverterProperties properties = new DefaultConverterProperties();
    properties.setPlatformVersion(targetVersion);
    ConverterProperties mergedProperties =
        ConverterPropertiesFactory.getInstance().merge(properties);
    return loadAndCheck(bpmnFile, mergedProperties);
  }

  public static DiagramCheckResult loadAndCheck(String bpmnFile, ConverterProperties properties) {
    DiagramConverter converter = DiagramConverterFactory.getInstance().get();
    BpmnModelInstance modelInstance = loadModelInstance(bpmnFile);

    DiagramCheckResult result = converter.check(bpmnFile, modelInstance, properties);
    return result;
  }

  private static BpmnModelInstance loadModelInstance(String bpmnFile) {
    return Bpmn.readModelFromStream(TestUtil.class.getClassLoader().getResourceAsStream(bpmnFile));
  }
}
