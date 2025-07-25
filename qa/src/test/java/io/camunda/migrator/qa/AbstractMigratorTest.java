/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.migrator.qa;

import io.camunda.migrator.impl.persistence.IdKeyMapper;
import io.camunda.migrator.qa.util.ProcessDefinitionDeployer;
import io.camunda.migrator.qa.util.WithMultiDb;
import io.camunda.migrator.qa.util.WithSpringProfile;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@WithMultiDb
@WithSpringProfile("history-level-full")
public class AbstractMigratorTest {

  @Autowired
  protected ProcessDefinitionDeployer deployer;

  @Autowired
  protected IdKeyMapper idKeyMapper;

  // C7 ---------------------------------------

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected TaskService taskService;
}
