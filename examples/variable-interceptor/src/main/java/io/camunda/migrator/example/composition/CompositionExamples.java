/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.example.composition;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import io.camunda.migrator.interceptor.composition.InterceptorCompositionUtilities;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
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
    public void execute(EntityConversionContext<?, ?> context) {
      // Manual type checking - boilerplate
      if (!(context.getC7Entity() instanceof HistoricProcessInstance processInstance)) {
        return;
      }

      // With the new DbModel approach, you would get the current model,
      // modify it, and set it back
      // This is just an example - the actual implementation depends on your needs
    }
  }

  // ========================================
  // AFTER: Using Composition Utilities
  // ========================================

  /**
   * PATTERN 1: Type-Safe Interceptor
   * Eliminates instanceof checks and provides compile-time type safety
   */
  public static EntityInterceptor createProcessInstanceTreePathCalculator() {
    return InterceptorCompositionUtilities.typeSafe(
        HistoricProcessInstance.class,
        context -> {
          HistoricProcessInstance pi = context.getEntity(); // Type-safe!

          // Get current DbModel, modify it, and set it back
          // Example: calculate tree path based on parent relationship
          String treePath = calculateTreePath(pi, context);
          // Store in metadata for other interceptors to use
          context.setMetadata("calculatedTreePath", treePath);
        }
    );
  }

  /**
   * PATTERN 2: Universal Cross-Cutting Concerns
   * Perfect for logging, metrics, auditing that applies to all entities
   */
  public static EntityInterceptor createAuditLogger() {
    return InterceptorCompositionUtilities.universal(context -> {
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
    return InterceptorCompositionUtilities.logger("[DEBUG]");
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
        InterceptorCompositionUtilities.logger("[PI-START]"),

        // 2. Set default properties
        createProcessInstancePropertyMapper(),

        // 3. Calculate derived properties
        createProcessInstanceTreePathCalculator(),

        // 4. Apply business rules conditionally
        createConditionalEnhancer(),

        // 5. Log completion
        InterceptorCompositionUtilities.logger("[PI-END]")
    );
  }

  /**
   * PATTERN 7: Multi-Type Interceptor
   * Handle related entity types with shared logic
   */
  public static EntityInterceptor createTenantNormalizer() {
    return InterceptorCompositionUtilities.universal(context -> {
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
