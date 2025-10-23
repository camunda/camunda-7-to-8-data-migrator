/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.example.composition;

import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.interceptor.composition.InterceptorCompositionUtilities;
import java.util.Map;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

/**
 * Example demonstrating how composition utilities simplify interceptor implementation.
 * These examples show the "before and after" of using composition utilities.
 */
@Component
public class CompositionExamples {

  // ========================================
  // BEFORE: Traditional Interceptor Implementation
  // ========================================

  /**
   * Traditional way - lots of boilerplate code
   */
  public static class TraditionalProcessInstanceInterceptor implements EntityInterceptor {
    @Override
    public Set<Class<?>> getEntityTypes() {
      return Set.of(HistoricProcessInstance.class);
    }

    @Override
    public int getOrder() {
      return 1000;
    }

    @Override
    public void execute(EntityConversionContext context) {
      // Manual type checking - boilerplate
      if (!(context.getC7Entity() instanceof HistoricProcessInstance processInstance)) {
        return;
      }

      // Manual property setting - repetitive
      context.setProperty("businessKey", processInstance.getBusinessKey());
      context.setProperty("startUserId", processInstance.getStartUserId());
      context.setProperty("superProcessInstanceId", processInstance.getSuperProcessInstanceId());
      // ... more properties
    }
  }

  // ========================================
  // AFTER: Using Composition Utilities
  // ========================================

  /**
   * PATTERN 1: Type-Safe Property Mapping
   * Eliminates instanceof checks and provides compile-time type safety
   */
  public static EntityInterceptor createProcessInstanceTreePathCalculator() {
    return InterceptorCompositionUtilities.typeSafe(
        HistoricProcessInstance.class,
        1000,
        context -> {
          HistoricProcessInstance pi = context.getEntity(); // Type-safe!

          // Calculate tree path based on parent relationship
          String treePath = calculateTreePath(pi, context);
          context.setProperty("treePath", treePath);
        }
    );
  }

  /**
   * PATTERN 2: Bulk Property Mapping
   * Reduces repetitive property setting to a declarative map
   */
  public static EntityInterceptor createProcessInstancePropertyMapper() {
    return InterceptorCompositionUtilities.propertyMapper(
        HistoricProcessInstance.class,
        500,
        Map.of(
            "businessKey", HistoricProcessInstance::getBusinessKey,
            "startUserId", HistoricProcessInstance::getStartUserId,
            "superProcessInstanceId", HistoricProcessInstance::getSuperProcessInstanceId,
            "deleteReason", HistoricProcessInstance::getDeleteReason,
            "duration", pi -> pi.getDurationInMillis()
        )
    );
  }

  /**
   * PATTERN 3: Universal Cross-Cutting Concerns
   * Perfect for logging, metrics, auditing that applies to all entities
   */
  public static EntityInterceptor createAuditLogger() {
    return InterceptorCompositionUtilities.universal(9000, context -> {
      String entityType = context.getEntityType().getSimpleName();
      String entityId = extractEntityId(context.getC7Entity());

      // Log conversion for audit trail
      auditLog.info("Converting {} with ID: {}", entityType, entityId);

      // Add audit metadata
      context.setMetadata("auditTimestamp", System.currentTimeMillis());
      context.setMetadata("auditUser", getCurrentUser());
    });
  }

  /**
   * PATTERN 4: Conditional Execution
   * Only run interceptor based on environment, feature flags, etc.
   */
  public static EntityInterceptor createConditionalEnhancer() {
    EntityInterceptor baseInterceptor = createProcessInstanceTreePathCalculator();

    return InterceptorCompositionUtilities.conditional(
        baseInterceptor,
        context -> {
          // Only calculate tree path in production or when feature flag is enabled
          return isProductionEnvironment() || isFeatureEnabled("TREE_PATH_CALCULATION");
        }
    );
  }

  /**
   * PATTERN 5: Debugging/Development Helper
   * Easily add logging at any point in the interceptor chain
   */
  public static EntityInterceptor createDebugLogger() {
    return InterceptorCompositionUtilities.logger("[DEBUG]", 50);
  }

  // ========================================
  // COMPARISON: Lines of Code Reduction
  // ========================================

  /**
   * Traditional approach: ~15-20 lines per interceptor
   * With composition utilities: ~3-5 lines per interceptor
   *
   * Benefits:
   * - 70% reduction in boilerplate code
   * - Type safety at compile time
   * - Consistent error handling
   * - Easier testing (can test logic separately from framework)
   * - More readable and maintainable
   */

  // ========================================
  // ADVANCED COMPOSITION PATTERNS
  // ========================================

  /**
   * PATTERN 6: Interceptor Chaining
   * Combine multiple simple interceptors into complex behavior
   */
  public static List<EntityInterceptor> createProcessInstanceChain() {
    return List.of(
        // 1. Log start of processing
        InterceptorCompositionUtilities.logger("[PI-START]", 100),

        // 2. Set default properties
        createProcessInstancePropertyMapper(),

        // 3. Calculate derived properties
        createProcessInstanceTreePathCalculator(),

        // 4. Apply business rules conditionally
        createConditionalEnhancer(),

        // 5. Log completion
        InterceptorCompositionUtilities.logger("[PI-END]", 9000)
    );
  }

  /**
   * PATTERN 7: Multi-Type Interceptor
   * Handle related entity types with shared logic
   */
  public static EntityInterceptor createTenantNormalizer() {
    return InterceptorCompositionUtilities.universal(200, context -> {
      String tenantId = extractTenantId(context.getC7Entity());
      String normalizedTenantId = normalizeTenantId(tenantId);
      context.setProperty("tenantId", normalizedTenantId);
    });
  }

  // Helper methods (implementation details omitted for brevity)
  private static String calculateTreePath(HistoricProcessInstance pi,
                                         InterceptorCompositionUtilities.TypeSafeContext<HistoricProcessInstance> context) {
    // Complex tree path calculation logic
    return "/" + pi.getId();
  }

  private static String extractEntityId(Object entity) {
    // Extract ID from various entity types
    return "unknown";
  }

  private static boolean isProductionEnvironment() {
    return "production".equals(System.getProperty("env"));
  }

  private static boolean isFeatureEnabled(String feature) {
    return Boolean.parseBoolean(System.getProperty("feature." + feature));
  }

  private static String getCurrentUser() {
    return System.getProperty("user.name");
  }

  private static String extractTenantId(Object entity) {
    // Extract tenant ID from various entity types
    return null;
  }

  private static String normalizeTenantId(String tenantId) {
    return tenantId != null ? tenantId : "default";
  }

  private static final org.slf4j.Logger auditLog =
      org.slf4j.LoggerFactory.getLogger("AUDIT");
}
