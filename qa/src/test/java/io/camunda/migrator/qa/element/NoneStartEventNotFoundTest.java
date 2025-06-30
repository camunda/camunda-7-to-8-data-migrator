/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.qa.element;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.RuntimeMigrator;
import io.camunda.migrator.qa.RuntimeMigrationAbstractTest;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoneStartEventNotFoundTest extends RuntimeMigrationAbstractTest {

  @RegisterExtension
  protected LogCapturer logs = LogCapturer.create().captureForType(RuntimeMigrator.class);

  @Test
  public void shouldNotSkipOnNoneStartEventPresent() {
    // given
    deployProcessInC7AndC8("noneStartProcess.bpmn");

    String id = runtimeService.startProcessInstanceByKey("noneStartProcess").getId();

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult()).isNotNull();

    // assume
    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(1);
  }

  @Test
  public void shouldSkipOnMissingNoneStartEvent() {
    // given
    deployProcessInC7AndC8("messageStartEventProcess.bpmn");
     runtimeService.correlateMessage("msgRef");

    // assume
    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(0);

    var events = logs.getEvents();
    assertThat(events.stream().filter(event -> event.getMessage()
        .matches(".*Couldn't find process None Start Event in C8 process with key.*")))
        .hasSize(1);
  }

  @Test
  public void shouldNotSkipOnMultipleStartEventsAndNoneStartEventPresent() {
    // given
    deployProcessInC7AndC8("multipleStartEvent.bpmn");

    String id = runtimeService.startProcessInstanceByKey("multipleStartEvent").getId();

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult()).isNotNull();

    // assume
    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(1);
  }

  @Test
  public void shouldSkipOnNoneStartEventOnlyExistInSubprocess() {
    // given
    deployProcessInC7AndC8("messageStartEventWithSubprocess.bpmn");

    String id = runtimeService.startProcessInstanceByKey("messageStartEventWithSubprocess").getId();

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult()).isNotNull();

    // assume
    // when
    runtimeMigrator.start();

    // then
    assertThatProcessInstanceCountIsEqualTo(0);

    var events = logs.getEvents();
    assertThat(events.stream().filter(event -> event.getMessage()
        .matches(".*Couldn't find process None Start Event in C8 process with key.*")))
        .hasSize(1);
  }
}
