/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.config;

import io.camunda.migrator.config.property.InterceptorProperty;
import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.exception.MigratorException;
import io.camunda.migrator.impl.logging.ConfigurationLogs;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for managing entity interceptors.
 */
@Configuration
public class EntityInterceptorConfiguration {

  @Autowired
  protected ApplicationContext context;

  @Autowired
  protected MigratorProperties migratorProperties;

  /**
   * Creates a composite list of entity interceptors from both Spring context and config data files.
   *
   * @return List of configured entity interceptors
   */
  @Bean
  public List<EntityInterceptor> configuredEntityInterceptors() {
    ConfigurationLogs.logConfiguringEntityInterceptors();

    // Get interceptors from Spring context (annotated with @Component)
    List<EntityInterceptor> contextInterceptors = new ArrayList<>(
        context.getBeansOfType(EntityInterceptor.class).values());

    // Handle unified interceptor configuration
    processUnifiedEntityInterceptorConfiguration(contextInterceptors, migratorProperties.getEntityInterceptors());


    ConfigurationLogs.logTotalEntityInterceptorsConfigured(contextInterceptors.size());
    return contextInterceptors;
  }

  /**
   * Processes unified entity interceptor configuration.
   *
   * @param contextInterceptors List of interceptors discovered from Spring context
   * @param interceptorConfigs List of interceptor configurations from config files
   */
  protected void processUnifiedEntityInterceptorConfiguration(List<EntityInterceptor> contextInterceptors,
                                                              List<InterceptorProperty> interceptorConfigs) {
    if (interceptorConfigs == null || interceptorConfigs.isEmpty()) {
      ConfigurationLogs.logNoEntityInterceptorsConfigured();
      return;
    }

    for (InterceptorProperty interceptorConfig : interceptorConfigs) {
      if (!interceptorConfig.isEnabled()) {
        handleEntityInterceptorDisable(contextInterceptors, interceptorConfig);
      } else {
        EntityInterceptor existingInterceptor = findExistingEntityInterceptor(contextInterceptors, interceptorConfig.getClassName());
        if (existingInterceptor != null) {
          ConfigurationLogs.logEntityInterceptorAlreadyLoaded(interceptorConfig.getClassName());
        } else {
          registerCustomEntityInterceptor(contextInterceptors, interceptorConfig);
        }
      }
    }
  }

  protected EntityInterceptor findExistingEntityInterceptor(List<EntityInterceptor> contextInterceptors, String className) {
    return contextInterceptors.stream()
        .filter(interceptor -> interceptor.getClass().getName().equals(className))
        .findFirst()
        .orElse(null);
  }

  protected void handleEntityInterceptorDisable(List<EntityInterceptor> contextInterceptors,
                                                InterceptorProperty interceptorConfig) {
    boolean removed = contextInterceptors.removeIf(interceptor ->
        interceptor.getClass().getName().equals(interceptorConfig.getClassName()));

    if (removed) {
      ConfigurationLogs.logEntityInterceptorDisabled(interceptorConfig.getClassName());
    } else {
      ConfigurationLogs.logEntityInterceptorNotFoundForDisabling(interceptorConfig.getClassName());
    }
  }

  protected void registerCustomEntityInterceptor(List<EntityInterceptor> contextInterceptors,
                                                 InterceptorProperty interceptorConfig) {
    try {
      EntityInterceptor interceptor = createEntityInterceptorInstance(interceptorConfig);
      contextInterceptors.add(interceptor);
      ConfigurationLogs.logEntityInterceptorSuccessfullyRegistered(interceptorConfig.getClassName());
    } catch (Exception e) {
      ConfigurationLogs.logFailedToRegisterEntityInterceptor(interceptorConfig.getClassName(), e);
      throw new MigratorException(ConfigurationLogs.getFailedToRegisterEntityInterceptorError(interceptorConfig.getClassName()), e);
    }
  }

  protected EntityInterceptor createEntityInterceptorInstance(InterceptorProperty interceptorProperty) throws Exception {
    String className = interceptorProperty.getClassName();
    if (className == null || className.trim().isEmpty()) {
      throw new IllegalArgumentException(ConfigurationLogs.getClassNameNullOrEmptyError());
    }

    ConfigurationLogs.logCreatingEntityInterceptorInstance(className);

    Class<?> clazz = Class.forName(className);
    if (!EntityInterceptor.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(ConfigurationLogs.getEntityInterceptorClassNotImplementInterfaceError(className));
    }

    EntityInterceptor interceptor = (EntityInterceptor) clazz.getDeclaredConstructor().newInstance();

    // Set properties if provided
    Map<String, Object> properties = interceptorProperty.getProperties();
    if (properties != null && !properties.isEmpty()) {
      ConfigurationLogs.logSettingEntityInterceptorProperties(className);
      applyProperties(interceptor, properties, false);
    }

    return interceptor;
  }

  protected static <T> void applyProperties(T target, Map<String, Object> sourceMap, boolean ignoreUnknownFields) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(sourceMap);
    Binder binder = new Binder(source);
    try {
      if (ignoreUnknownFields) {
        binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(target));
      } else {
        binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(target),
            new NoUnboundElementsBindHandler(BindHandler.DEFAULT));
      }
    } catch (Exception e) {
      throw new MigratorException(ConfigurationLogs.getParsingConfigurationError(), e);
    }
  }
}

