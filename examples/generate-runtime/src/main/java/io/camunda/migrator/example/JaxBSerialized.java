/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.migrator.example;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Daniel Meyer
 *
 */
@XmlRootElement
public class JaxBSerialized {

  protected String foo;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

}
