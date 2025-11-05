/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.interceptor.composition;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing common composition patterns for entity interceptors.
 * These utilities reduce boilerplate code and promote consistent interceptor implementation.
 */
public final class InterceptorCompositionUtilities {

  private static final Logger LOGGER = LoggerFactory.getLogger(InterceptorCompositionUtilities.class);

  private InterceptorCompositionUtilities() {
    // Utility class
  }

  /**
   * Creates a type-safe interceptor that only executes for specific entity types.
   * Eliminates the need for manual instanceof checks.
   *
   * @param entityType the target entity type
   * @param logic the conversion logic to execute
   * @param <T> the entity type
   * @return a type-safe interceptor
   */
  public static <T> EntityInterceptor typeSafe(Class<T> entityType,
                                               Consumer<TypeSafeContext<T>> logic) {
    return new EntityInterceptor() {
      @Override
      public Set<Class<?>> getEntityTypes() {
        return Set.of(entityType);
      }


      @Override
      public void execute(EntityConversionContext context) {
        if (entityType.isInstance(context.getC7Entity())) {
          @SuppressWarnings("unchecked")
          T typedEntity = (T) context.getC7Entity();
          logic.accept(new TypeSafeContext<>(typedEntity, context));
        }
      }
    };
  }

  /**
   * Creates a universal interceptor that executes for all entity types.
   * Commonly used for cross-cutting concerns like logging, auditing, metrics.
   *
   * @param logic the logic to execute
   * @return a universal interceptor
   */
  public static EntityInterceptor universal(Consumer<EntityConversionContext> logic) {
    return new EntityInterceptor() {
      @Override
      public Set<Class<?>> getEntityTypes() {
        return Set.of(); // Empty = handle all types
      }


      @Override
      public void execute(EntityConversionContext context) {
        logic.accept(context);
      }
    };
  }

  /**
   * Creates a conditional interceptor that only executes when a predicate is satisfied.
   * Useful for environment-specific logic or feature flags.
   *
   * @param baseInterceptor the interceptor to wrap
   * @param condition the condition to check
   * @return a conditional interceptor
   */
  public static EntityInterceptor conditional(EntityInterceptor baseInterceptor,
                                              Predicate<EntityConversionContext> condition) {
    return new EntityInterceptor() {
      @Override
      public Set<Class<?>> getEntityTypes() {
        return baseInterceptor.getEntityTypes();
      }


      @Override
      public void execute(EntityConversionContext context) {
        if (condition.test(context)) {
          baseInterceptor.execute(context);
        }
      }
    };
  }

  /**
   * Creates a property mapping interceptor for bulk property setting.
   * Reduces boilerplate for simple property mappings.
   *
   * @param entityType the target entity type
   * @param propertyMappings map of property names to extraction functions
   * @param <T> the entity type
   * @return a property mapping interceptor
   */
  public static <T> EntityInterceptor propertyMapper(Class<T> entityType,
                                                     Map<String, Function<T, Object>> propertyMappings) {
    return typeSafe(entityType, context -> {
      T entity = context.getEntity();
      propertyMappings.forEach((propertyName, extractor) -> {
        try {
          Object value = extractor.apply(entity);
          context.setProperty(propertyName, value);
        } catch (Exception e) {
          LOGGER.warn("Failed to extract property '{}' from {}: {}",
                     propertyName, entityType.getSimpleName(), e.getMessage());
        }
      });
    });
  }

  /**
   * Creates a logging interceptor for debugging conversion chains.
   *
   * @param prefix log message prefix
   * @return a logging interceptor
   */
  public static EntityInterceptor logger(String prefix) {
    return universal(context -> {
      String entityType = context.getEntityType().getSimpleName();
      LOGGER.info("{} Processing {} conversion", prefix, entityType);

      // Log current properties for debugging
      if (LOGGER.isDebugEnabled()) {
        context.getProperties().forEach((key, value) ->
            LOGGER.debug("{} Property '{}' = {}", prefix, key, value));
      }
    });
  }

  /**
   * Type-safe context wrapper that provides strongly-typed access to the entity.
   */
  public static class TypeSafeContext<T> {
    private final T entity;
    private final EntityConversionContext<?> context;

    public TypeSafeContext(T entity, EntityConversionContext<?> context) {
      this.entity = entity;
      this.context = context;
    }

    public T getEntity() {
      return entity;
    }

    public Object getProperty(String propertyName) {
      return context.getProperty(propertyName);
    }

    public void setProperty(String propertyName, Object value) {
      context.setProperty(propertyName, value);
    }

    public void nullifyProperty(String propertyName) {
      context.nullifyProperty(propertyName);
    }

    public Object getMetadata(String key) {
      return context.getMetadata(key);
    }

    public void setMetadata(String key, Object value) {
      context.setMetadata(key, value);
    }
  }
}
