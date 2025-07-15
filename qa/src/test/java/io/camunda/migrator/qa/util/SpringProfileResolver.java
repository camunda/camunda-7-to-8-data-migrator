/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.util;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.test.context.ActiveProfilesResolver;

/**
 * This class will append the required profile with the profiles already set in spring.profiles.active
 */
public final class SpringProfileResolver implements ActiveProfilesResolver {

  @Override
  public String @NotNull [] resolve(@NotNull Class<?> testClass) {
    WithSpringProfile annotation = AnnotationUtils.findAnnotation(testClass, WithSpringProfile.class).orElse(null);
    String activeProfiles = System.getProperty("spring.profiles.active", "");
    String profile = annotation == null ? "" : annotation.value();
    activeProfiles = activeProfiles + "," + profile;
    return Arrays.stream(activeProfiles.split("\\s*,\\s*")).toArray(String[]::new);
  }
}