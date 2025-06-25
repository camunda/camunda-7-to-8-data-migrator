/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InterceptorService {

  List<VariableInterceptor> interceptors;

  @Autowired
  public InterceptorService(List<VariableInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public List<VariableInterceptor> getInterceptors() {
    return interceptors;
  }
}
