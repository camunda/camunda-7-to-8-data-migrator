/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test to verify that IdKeyDbModel createTime is mapped correctly across different database types
 * when running actual migration queries defined in IdKey.xml.
 */
public class IdKeyCreateTimeMappingTest extends HistoryMigrationAbstractTest {

  @Autowired
  private IdKeyMapper idKeyMapper;

  @Test
  public void shouldCorrectlyMapCreateTimeDuringActualMigration() {

    // Given
    deployer.deployCamunda7Process("simpleProcess.bpmn");
    String processInstanceId = runtimeService.startProcessInstanceByKey("simpleProcess").getId();
    // Record the time before migration for comparison
    Date beforeMigration = new Date();

    // When: Run the history migration
    historyMigrator.migrate();

    // Then: Verify that migrated instances have correct mapping
    IdKeyDbModel migratedInstance = idKeyMapper.findMigratedByType(
        IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE, 0, Integer.MAX_VALUE).stream().toList().getFirst();

    assertThat(migratedInstance.getC7Id()).isEqualTo(processInstanceId);
    assertThat(migratedInstance.getCreateTime()).isNotNull().isBefore(beforeMigration); // Should be before we started the test
    assertThat(migratedInstance.getC8Key()).isNotNull().isPositive();
  }
}
