/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(MigratorProperties.PREFIX)
public class MigratorProperties {

  public static final String PREFIX = "migrator";
  public static final int MAX_BATCH_SIZE = 500;

  protected Integer batchSize = MAX_BATCH_SIZE;

  public int getBatchSize() {
    return batchSize;
  }
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

}
