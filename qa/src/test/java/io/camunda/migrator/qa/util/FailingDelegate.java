/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.util;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class FailingDelegate implements JavaDelegate {

  public static final String EXCEPTION_MESSAGE = "Expected_exception.";

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    Boolean fail = (Boolean) execution.getVariable("fail");
    String message = execution.hasVariable("message") ?
        (String) execution.getVariable("message") : EXCEPTION_MESSAGE;

    if (fail == null || fail == true) {
      throw new ProcessEngineException(message);
    }

  }

}
