/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.example;

import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication {

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected TaskService taskService;

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  @PostConstruct
  public void generateRuntimeData() {
    runtimeService.startProcessInstanceByKey("cornercasesProcess", Variables.putValue("myGlobalVar", 1234)).getId();
    taskService.createTaskQuery().processDefinitionKey("complex-child-process").list().forEach(task -> taskService.complete(task.getId()));
  }

}
