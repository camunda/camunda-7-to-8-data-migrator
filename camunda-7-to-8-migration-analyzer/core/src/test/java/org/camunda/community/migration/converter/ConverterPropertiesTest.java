/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ConverterPropertiesTest {
  @Test
  void shouldContainDefaultValues() {
    ConverterProperties properties = ConverterPropertiesFactory.getInstance().get();
    assertThat(properties.getScriptHeader()).isEqualTo("script");
    assertThat(properties.getResultVariableHeader()).isEqualTo("resultVariable");
    assertThat(properties.getDefaultJobType()).isEqualTo("camunda-7-adapter");
    assertThat(properties.getScriptJobType()).isEqualTo("script");
    assertThat(properties.getResourceHeader()).isEqualTo("resource");
    assertThat(properties.getScriptFormatHeader()).isEqualTo("language");
    assertThat(properties.getPlatformVersion()).isNotNull();
    assertThat(properties.getKeepJobTypeBlank()).isFalse();
    assertThat(properties.getAlwaysUseDefaultJobType()).isFalse();
    assertThat(properties.getAddDataMigrationExecutionListener()).isFalse();
    assertThat(properties.getDataMigrationExecutionListenerJobType())
        .isEqualTo("=if legacyId != null then \"migrator\" else \"noop\"");
    assertThat(properties.getAppendDocumentation()).isFalse();
    assertThat(properties.getAppendElements()).isTrue();
  }

  @Test
  void shouldMergeProperties() {
    DefaultConverterProperties properties = new DefaultConverterProperties();
    properties.setDefaultJobType("adapter");
    assertNull(properties.getResourceHeader());
    ConverterProperties converterProperties =
        ConverterPropertiesFactory.getInstance().merge(properties);
    assertEquals("adapter", converterProperties.getDefaultJobType());
    assertNotNull(converterProperties.getResourceHeader());
  }
}
