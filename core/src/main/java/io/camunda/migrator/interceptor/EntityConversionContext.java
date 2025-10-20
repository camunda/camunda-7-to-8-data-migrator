
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
 * and the converted properties for the C8 database model.
 * <p>
 * This context allows interceptors to:
 * - Access the original C7 entity
 * - Read current property values
 * - Override property values
 * - Nullify property values
 * - Add custom metadata
 * </p>
 *
 * @param <T> the C7 entity type
 */
public class EntityConversionContext<T> {

  private final T c7Entity;
  private final Map<String, Object> properties;
  private final Map<String, Object> metadata;
  private final Class<?> entityType;

  public EntityConversionContext(T c7Entity, Class<?> entityType) {
    if (c7Entity == null) {
      throw new IllegalArgumentException("C7 entity cannot be null");
    }
    this.c7Entity = c7Entity;
    this.entityType = entityType;
    this.properties = new HashMap<>();
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
  public T getC7Entity() {
    return c7Entity;
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
   * Gets the current value of a property.
   *
   * @param propertyName the property name
   * @return the property value, or null if not set
   */
  public Object getProperty(String propertyName) {
    return properties.get(propertyName);
  }

  /**
   * Sets a property value for the C8 model.
   * This overrides any previously set value.
   *
   * @param propertyName the property name
   * @param value        the value to set
   */
  public void setProperty(String propertyName, Object value) {
    properties.put(propertyName, value);
  }

  /**
   * Nullifies a property value.
   * This is different from not setting it - it explicitly sets it to null.
   *
   * @param propertyName the property name
   */
  public void nullifyProperty(String propertyName) {
    properties.put(propertyName, null);
  }

  /**
   * Removes a property completely.
   * After this, the property will not be included in the C8 model.
   *
   * @param propertyName the property name
   */
  public void removeProperty(String propertyName) {
    properties.remove(propertyName);
  }

  /**
   * Checks if a property has been set.
   *
   * @param propertyName the property name
   * @return true if the property has been set, false otherwise
   */
  public boolean hasProperty(String propertyName) {
    return properties.containsKey(propertyName);
  }

  /**
   * Gets all properties as a map.
   *
   * @return unmodifiable view of all properties
   */
  public Map<String, Object> getProperties() {
    return new HashMap<>(properties);
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

  /**
   * Gets custom metadata.
   *
   * @param key the metadata key
   * @return the metadata value, or null if not set
   */

}