
/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor;

/**
 * Utility class for determining entity type compatibility with interceptors.
 */
public final class EntityTypeDetector {

  private EntityTypeDetector() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if an interceptor supports a specific entity type.
   *
   * @param interceptor the interceptor to check
   * @param entityType the entity type to check
   * @return true if the interceptor supports the entity type
   */
  public static boolean supportsEntityType(EntityInterceptor interceptor, Class<?> entityType) {
    var supportedTypes = interceptor.getEntityTypes();

    // Empty set means handle all types
    if (supportedTypes.isEmpty()) {
      return true;
    }

    // Check if any supported type matches the entity type
    return supportedTypes.stream()
        .anyMatch(supportedType -> supportedType.isAssignableFrom(entityType));
  }

  /**
   * Checks if an interceptor supports a specific entity instance.
   *
   * @param interceptor the interceptor to check
   * @param entity the entity instance to check
   * @return true if the interceptor supports the entity
   */
  public static boolean supportsEntity(EntityInterceptor interceptor, Object entity) {
    if (entity == null) {
      return false;
    }
    return supportsEntityType(interceptor, entity.getClass());
  }

  /**
   * Checks if an interceptor supports a specific conversion context.
   *
   * @param interceptor the interceptor to check
   * @param context the conversion context
   * @return true if the interceptor supports the context's entity type
   */
  public static boolean supportsContext(EntityInterceptor interceptor, EntityConversionContext<?, ?> context) {
    return supportsEntityType(interceptor, context.getEntityType());
  }
}
