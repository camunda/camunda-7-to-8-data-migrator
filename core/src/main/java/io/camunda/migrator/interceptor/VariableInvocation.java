/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor;

import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

/**
 * Represents an invocation of a variable within a process instance.
 */
public class VariableInvocation {

  protected VariableInstanceEntity variable;
  protected String processInstanceId;
  protected String activityInstanceId;
  protected String variableValue;

  public VariableInvocation(VariableInstanceEntity variable) {
    this.variable = variable;
    this.processInstanceId = variable.getProcessInstanceId();
    this.activityInstanceId = variable.getActivityInstanceId();
  }

  /**
   * Returns the variable instance entity associated with this invocation.
   *
   * @return the {@link VariableInstanceEntity}
   */
  public VariableInstanceEntity getVariable() {
    return variable;
  }

  /**
   * Returns the process instance ID associated with this variable invocation.
   *
   * @return the process instance ID
   */
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  /**
   * Returns the activity instance ID associated with this variable invocation.
   *
   * @return the activity instance ID
   */
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public String getVariableValue() {
    return variableValue;
  }

  public void setVariableValue(String variableValue) {
    this.variableValue = variableValue;
  }
}
