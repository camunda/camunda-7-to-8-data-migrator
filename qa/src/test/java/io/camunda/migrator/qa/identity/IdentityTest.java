package io.camunda.migrator.qa.identity;

import io.camunda.migrator.IdentityMigrator;
import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class IdentityTest extends HistoryMigrationAbstractTest {

  @Autowired
  private IdentityMigrator identityMigrator;

  @Test
  public void test() {
    identityMigrator.migrate();
  }

}
