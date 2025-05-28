/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.camunda.client.api.search.response.ProcessInstance;

@ExtendWith(OutputCaptureExtension.class)
class BatchConfigurationTest extends RuntimeMigrationAbstractTest {

  @Test
  public void shouldPerformPaginationForProcessInstances(CapturedOutput output) {
    runtimeMigrator.setBatchSize(2);
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("simpleProcess");
    }
    assertThat(runtimeService.createProcessInstanceQuery().list().size()).isEqualTo(5);

    // when running runtime migration
    runtimeMigrator.migrate();

    // then
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertThat(processInstances.size()).isEqualTo(5);
    assertThat(output.getOut()).contains("Max count: 5, offset: 0, batch size: 2");
    assertThat(output.getOut()).contains("Max count: 5, offset: 2, batch size: 2");
    assertThat(output.getOut()).contains("Max count: 5, offset: 4, batch size: 2");
  }

  @Test
  public void shouldPerformPaginationForMigrationJobs(CapturedOutput output) {
    runtimeMigrator.setBatchSize(2);
    // deploy processes
    deployProcessInC7AndC8("simpleProcess.bpmn");

    // given
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("simpleProcess");
    }

    // when running runtime migration
    runtimeMigrator.migrate();

    // then
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertThat(processInstances.size()).isEqualTo(5);

    Matcher matcher = Pattern.compile("Migrator jobs found: 2").matcher(output.getOut());
    assertThat(matcher.results().count()).isEqualTo(2);
    assertThat(output.getOut()).contains("Migrator jobs found: 1");
    assertThat(output.getOut()).contains("No more migrator jobs available.");
  }


}
