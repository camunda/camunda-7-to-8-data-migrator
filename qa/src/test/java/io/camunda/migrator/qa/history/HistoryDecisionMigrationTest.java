/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.qa.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.search.entities.DecisionDefinitionEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HistoryDecisionMigrationTest extends HistoryMigrationAbstractTest {

  @Test
  public void migrateHistoricDecision() {
    // given
    deployer.deployCamunda7Decision("simpleDmn.dmn");

    // when
    historyMigrator.migrate();

    // then
    List<DecisionDefinitionEntity> migratedDecisions = searchHistoricDecisionDefinitions("simpleDmnId");
    assertThat(migratedDecisions).singleElement()
        .satisfies(decision -> {
          assertThat(decision.decisionDefinitionId()).isEqualTo("simpleDmnId");
          assertThat(decision.decisionDefinitionKey()).isNotNull();
          assertThat(decision.version()).isEqualTo(1);
          assertThat(decision.name()).isEqualTo("simpleDmnName");
//          assertThat(decision.tenantId()).isEqualTo("<default>");  TODO comes with other PR?
          assertThat(decision.decisionRequirementsKey()).isNotNull();
          assertThat(decision.decisionRequirementsId()).isEqualTo("aDmnId");
        });
  }
}
