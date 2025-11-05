/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor;

import java.util.Set;

/**
 * Interceptor interface for handling historic entity property conversions during migration.
 * <p>
 * Implement this interface to define custom logic for converting C7 historic entities
 * to C8 database models. Interceptors can specify which entity types they handle,
 * allowing the system to only call relevant interceptors.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 *
 * <h3>Type-specific interceptor:</h3>
 * <pre>
 * &#64;Component
 * public class ProcessInstanceTreePathInterceptor implements EntityInterceptor {
 *   &#64;Override
 *   public Set&lt;Class&lt;?&gt;&gt; getEntityTypes() {
 *     return Set.of(HistoricProcessInstance.class);
 *   }
 *
 *   &#64;Override
 *   public void execute(EntityConversionContext&lt;?, ?&gt; context) {
 *     // Only called for HistoricProcessInstance entities
 *     if (context.getC7Entity() instanceof HistoricProcessInstance pi) {
 *       // Get the current model and create a modified version
 *       ProcessInstanceDbModel currentModel = (ProcessInstanceDbModel) context.getC8DbModel();
 *       String treePath = calculateTreePath(pi);
 *       ProcessInstanceDbModel updatedModel = currentModel.toBuilder()
 *           .treePath(treePath)
 *           .build();
 *       context.setC8DbModel(updatedModel);
 *     }
 *   }
 * }
 * </pre>
 *
 * <h3>Universal interceptor (handles all entity types):</h3>
 * <pre>
 * &#64;Component
 * public class AuditLoggingInterceptor implements EntityInterceptor {
 *   &#64;Override
 *   public Set&lt;Class&lt;?&gt;&gt; getEntityTypes() {
 *     return Set.of(); // Empty set = handle all types
 *   }
 *
 *   &#64;Override
 *   public void execute(EntityConversionContext&lt;?, ?&gt; context) {
 *     // Called for all entity types
 *     logEntityConversion(context.getC7Entity(), context.getC8DbModel());
 *   }
 * }
 * </pre>
 *
 * <h3>Disabling interceptors via YAML configuration:</h3>
 * <pre>
 * migrator:
 *   entity-interceptors:
 *     - className: "io.camunda.migrator.impl.interceptor.DefaultProcessInstanceConverter"
 *       enabled: false
 * </pre>
 */
public interface EntityInterceptor {

  /**
   * Executes the interceptor logic for an entity conversion.
   * This method will only be called if the entity type matches one of the supported types.
   *
   * @param context the entity conversion context containing C7 entity data and C8 model builder
   */
  void execute(EntityConversionContext<?, ?> context);

  /**
   * Returns the set of entity types that this interceptor can handle.
   * <p>
   * Use Camunda 7 historic entity classes like:
   * - {@code HistoricProcessInstance.class} for process instances
   * - {@code HistoricActivityInstance.class} for flow nodes/activities
   * - {@code HistoricVariableInstance.class} for variables
   * - {@code HistoricTaskInstance.class} for user tasks
   * - {@code HistoricIncident.class} for incidents
   * - {@code HistoricDecisionInstance.class} for decision instances
   * </p>
   * <p>
   * If the returned set is empty, this interceptor will be called for all entity types.
   * </p>
   * <p>
   * Default implementation returns an empty set (handle all types) for backward compatibility.
   * </p>
   *
   * @return set of supported entity types, or empty set to handle all types
   */
  default Set<Class<?>> getEntityTypes() {
    return Set.of(); // Empty set = handle all types for backward compatibility
  }
}
