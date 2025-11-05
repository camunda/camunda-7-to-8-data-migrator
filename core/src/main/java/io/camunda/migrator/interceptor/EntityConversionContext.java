/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for entity conversion that holds both the C7 historic entity
 * and the C8 database model being built.
 * <p>
 * This context allows interceptors to:
 * - Access the original C7 entity
 * - Access and modify the C8 database model
 * - Add custom metadata
 * </p>
 *
 * @param <C7> the C7 entity type
 * @param <C8> the C8 database model type
 */
public class EntityConversionContext<C7, C8> {

  private final C7 c7Entity;
  private C8 c8DbModel;
  private final Map<String, Object> metadata;
  private final Class<?> entityType;

  public EntityConversionContext(C7 c7Entity, Class<?> entityType) {
    if (c7Entity == null) {
      throw new IllegalArgumentException("C7 entity cannot be null");
    }
    this.c7Entity = c7Entity;
    this.entityType = entityType;
    this.c8DbModel = null;
    this.metadata = new HashMap<>();
  }

  public EntityConversionContext(C7 c7Entity, Class<?> entityType, C8 c8DbModel) {
    if (c7Entity == null) {
      throw new IllegalArgumentException("C7 entity cannot be null");
    }
    this.c7Entity = c7Entity;
    this.entityType = entityType;
    this.c8DbModel = c8DbModel;
    this.metadata = new HashMap<>();
  }

  public Object getMetadata(String key) {
    return metadata.get(key);
  }

  /**
   * Checks if metadata exists.
   *
   * @param key the metadata key
   * @return true if metadata exists, false otherwise
   */
  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  /**
   * Returns the C7 historic entity being converted.
   *
   * @return the C7 entity
   */
  public C7 getC7Entity() {
    return c7Entity;
  }

  /**
   * Returns the C8 database model being built.
   *
   * @return the C8 database model, or null if not yet set
   */
  public C8 getC8DbModel() {
    return c8DbModel;
  }

  /**
   * Sets the C8 database model.
   * Interceptors can use this to provide or update the database model.
   *
   * @param c8DbModel the C8 database model
   */
  public void setC8DbModel(C8 c8DbModel) {
    this.c8DbModel = c8DbModel;
  }

  /**
   * Returns the entity type class.
   *
   * @return the entity type
   */
  public Class<?> getEntityType() {
    return entityType;
  }

  /**
   * Sets custom metadata for this conversion.
   * Metadata is not included in the C8 model but can be used
   * by interceptors to share information.
   *
   * @param key   the metadata key
   * @param value the metadata value
   */
  public void setMetadata(String key, Object value) {
    metadata.put(key, value);
  }

}