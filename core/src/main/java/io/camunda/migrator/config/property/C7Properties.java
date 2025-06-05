/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(C7Properties.PREFIX)
public class C7Properties {

  public static final String PREFIX = "c7";

  protected Boolean autoDdl;

  public Boolean getAutoDdl() {
    return autoDdl;
  }

  public void setAutoDdl(Boolean autoDdl) {
    this.autoDdl = autoDdl;
  }


}
