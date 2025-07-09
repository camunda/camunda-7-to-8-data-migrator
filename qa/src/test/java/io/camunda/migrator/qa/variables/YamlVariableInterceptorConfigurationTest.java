/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.variables;

import static io.camunda.migrator.MigratorMode.RETRY_SKIPPED;
import static io.camunda.process.test.api.assertions.ElementSelectors.byId;
import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.interceptor.VariableInterceptor;
import io.camunda.migrator.qa.RuntimeMigrationAbstractTest;
import io.camunda.process.test.api.CamundaAssert;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("interceptor-test")
@SpringBootTest
public class YamlVariableInterceptorConfigurationTest extends RuntimeMigrationAbstractTest {

  @Autowired
  private MigratorProperties migratorProperties;

  @Autowired
  private List<VariableInterceptor> configuredVariableInterceptors;

  @Test
  public void shouldLoadVariableInterceptorFromYamlConfiguration() {
    // Verify that the YAML configuration is loaded correctly
    assertThat(migratorProperties.getVariableInterceptorPlugins()).isNotNull();
    assertThat(migratorProperties.getVariableInterceptorPlugins()).hasSize(1);

    var pluginConfig = migratorProperties.getVariableInterceptorPlugins().get(0);
    assertThat(pluginConfig.getClassName()).isEqualTo("io.camunda.migrator.qa.variables.YamlConfiguredVariableInterceptor");
    assertThat(pluginConfig.getProperties()).containsEntry("prefix", "YAML_TEST_");
    assertThat(pluginConfig.getProperties()).containsEntry("targetVariable", "yamlVar");
  }

  @Test
  public void shouldRegisterYamlConfiguredInterceptorInInterceptorsList() {
    // Verify that the YAML-configured interceptor is included in the configured interceptors list
    assertThat(configuredVariableInterceptors).isNotNull();

    boolean hasYamlInterceptor = configuredVariableInterceptors.stream()
        .anyMatch(interceptor -> interceptor instanceof YamlConfiguredVariableInterceptor);

    assertThat(hasYamlInterceptor).isTrue();

    // Find the YAML interceptor and verify its configuration
    YamlConfiguredVariableInterceptor yamlInterceptor = configuredVariableInterceptors.stream()
        .filter(interceptor -> interceptor instanceof YamlConfiguredVariableInterceptor)
        .map(YamlConfiguredVariableInterceptor.class::cast)
        .findFirst()
        .orElse(null);

    assertThat(yamlInterceptor).isNotNull();
    assertThat(yamlInterceptor.getPrefix()).isEqualTo("YAML_TEST_");
    assertThat(yamlInterceptor.getLogMessage()).isEqualTo("Hello from YAML interceptor configured via properties");
    assertThat(yamlInterceptor.isEnableTransformation()).isTrue();
    assertThat(yamlInterceptor.getTargetVariable()).isEqualTo("yamlVar");
  }

  @Test
  public void shouldInvokeYamlConfiguredInterceptorDuringMigration(CapturedOutput output) {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");
    deployProcessInC7AndC8("userTaskProcess.bpmn");

    // given processes state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "yamlVar", "originalValue");

    var userTaskProcessInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    runtimeService.setVariable(userTaskProcessInstance.getId(), "yamlVar", "anotherValue");

    // when running runtime migration
    runtimeMigrator.start();

    // then verify that variables were transformed by the YAML-configured interceptor
    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("yamlVar", "YAML_TEST_originalValue");
    CamundaAssert.assertThat(byProcessId("userTaskProcessId"))
        .hasVariable("yamlVar", "YAML_TEST_anotherValue");

    // verify interceptor log messages appear
    assertThat(output.getOut()).contains("Hello from YAML interceptor configured via properties");
    Matcher matcher = Pattern.compile("Hello from YAML interceptor configured via properties").matcher(output.getOut());
    assertThat(matcher.results().count()).isEqualTo(2);

    // verify transformation log messages
    assertThat(output.getOut()).contains("Transformed variable yamlVar from 'originalValue' to 'YAML_TEST_originalValue'");
    assertThat(output.getOut()).contains("Transformed variable yamlVar from 'anotherValue' to 'YAML_TEST_anotherValue'");
  }

  @Test
  public void shouldSkipProcessInstanceDueToExceptionFromYamlInterceptor(CapturedOutput output) {
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given processes state in c7
    var simpleProcessInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(simpleProcessInstance.getId(), "yamlExFlag", true);

    // run migration first time
    runtimeMigrator.start();

    assertThat(output.getOut()).contains("Skipping process instance with legacyId:");
    assertThat(output.getOut()).contains("due to: An error occurred during variable transformation");

    // fix the variable to allow successful migration
    runtimeService.setVariable(simpleProcessInstance.getId(), "yamlExFlag", false);

    // when run runtime migration again with RETRY_SKIPPED mode
    runtimeMigrator.setMode(RETRY_SKIPPED);
    runtimeMigrator.start();

    // then
    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasActiveElements(byId("userTask1"))
        .hasVariable("yamlExFlag", false);

    assertThat(output.getOut()).contains("Success from YAML interceptor");
  }

  @Test
  public void shouldWorkAlongsideSpringComponentInterceptors(CapturedOutput output) {
    // This test verifies that YAML-configured interceptors work alongside Spring @Component interceptors
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // Set up variables that will trigger both types of interceptors
    var processInstance = runtimeService.startProcessInstanceByKey("simpleProcess");
    runtimeService.setVariable(processInstance.getId(), "varIntercept", "springValue"); // For TestVariableInterceptor (@Component)
    runtimeService.setVariable(processInstance.getId(), "yamlVar", "yamlValue"); // For YamlConfiguredVariableInterceptor (YAML)

    // when running runtime migration
    runtimeMigrator.start();

    // then both interceptors should have executed
    CamundaAssert.assertThat(byProcessId("simpleProcess"))
        .hasVariable("varIntercept", "Hello") // Transformed by Spring @Component interceptor
        .hasVariable("yamlVar", "YAML_TEST_yamlValue"); // Transformed by YAML interceptor

    // verify both interceptors logged their messages
    assertThat(output.getOut()).contains("Hello from interceptor"); // From TestVariableInterceptor
    assertThat(output.getOut()).contains("Hello from YAML interceptor configured via properties"); // From YAML interceptor
  }
}
