package io.camunda.migrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class C7MigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(C7MigrationService.class);

  public void migrate() {
    LOGGER.info("Migrating C7 data...");
  }

}