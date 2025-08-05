/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.MigratorMode;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import org.junit.jupiter.api.Test;

public class HistoryMigrationRetryTest extends HistoryMigrationAbstractTest {

    @Test
    public void shouldMigratePreviouslySkippedProcessDefinition() {
        // given state in c7
        deployer.deployCamunda7Process("userTaskProcess.bpmn");

        // and the process definition is manually set as skipped
        String legacyId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        dbClient.insert(legacyId, null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);

        // when history migration is retried
        historyMigrator.setMode(MigratorMode.RETRY_SKIPPED);
        historyMigrator.migrate();

        // then process definition is migrated and no longer skipped
        assertThat(searchHistoricProcessDefinitions("userTaskProcessId").size()).isEqualTo(1);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION)).isEqualTo(0);
    }
}
