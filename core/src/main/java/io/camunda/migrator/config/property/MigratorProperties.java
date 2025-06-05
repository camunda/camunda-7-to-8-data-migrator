/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config.property;

import io.camunda.migrator.RuntimeMigrator;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(MigratorProperties.PREFIX)
public class MigratorProperties {

  public static final String PREFIX = "migrator";

  protected Integer batchSize = RuntimeMigrator.DEFAULT_BATCH_SIZE;

  protected DataSource source;

  protected DataSource target;

  @NestedConfigurationProperty
  protected C7Properties c7Properties;

  @NestedConfigurationProperty
  protected RdbmsExporterProperties rdbmsExporterProperties;

  public int getBatchSize() {
    return batchSize;
  }
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public DataSource getSource() {
    return source;
  }

  public void setSource(DataSource source) {
    this.source = source;
  }

  public DataSource getTarget() {
    return target;
  }

  public void setTarget(DataSource target) {
    this.target = target;
  }

  public C7Properties getC7Properties() {
    return c7Properties;
  }

  public void setC7Properties(C7Properties c7Properties) {
    this.c7Properties = c7Properties;
  }

  public RdbmsExporterProperties getRdbmsExporterProperties() {
    return rdbmsExporterProperties;
  }

  public void setRdbmsExporterProperties(RdbmsExporterProperties rdbmsExporterProperties) {
    this.rdbmsExporterProperties = rdbmsExporterProperties;
  }
}
