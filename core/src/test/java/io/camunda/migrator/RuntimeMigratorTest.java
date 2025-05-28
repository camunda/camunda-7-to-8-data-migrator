/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.migrator.history.IdKeyMapper;
import java.util.Collections;
import org.apache.ibatis.exceptions.PersistenceException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.VariableInstanceQueryImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeMigratorTest {

  @InjectMocks
  private RuntimeMigrator runtimeMigrator;

  @Spy
  private RuntimeService runtimeService;

  @Mock
  private CamundaClient camundaClient;

  @Mock
  private IdKeyMapper idKeyMapper;

  @Mock
  private ProcessInstanceQueryImpl processInstanceQuery;

  @Test
  public void shouldMigrationExceptionBeThrownOnPersistenceException() {
    // given exception is thrown by idKeyMapper
    PersistenceException caughtException = new PersistenceException("Persistence error");
    when(idKeyMapper.findLatestIdByType(anyString())).thenThrow(caughtException);

    // when migrating, then exception is wrapped and migration halted
    MigratorException migratorException = assertThrows(MigratorException.class, () -> runtimeMigrator.migrate());
    assertEquals(caughtException, migratorException.getCause());
    assertTrue(migratorException.getMessage().contains("Error while fetching instances to migrate"));
    verifyNoMoreInteractions(idKeyMapper);
    verifyNoInteractions(runtimeService);
    verifyNoInteractions(camundaClient);
  }

  @Test
  public void shouldMigrationExceptionBeThrownOnProcessEngineException() {
    // given exception is thrown by C7 api
    ProcessEngineException caughtException = new ProcessEngineException("Process engine error");
    when(idKeyMapper.findLatestIdByType(anyString())).thenReturn("id");
    when(runtimeService.createProcessInstanceQuery()).thenThrow(caughtException);

    // when migrating, then exception is wrapped and migration halted
    MigratorException migratorException = assertThrows(MigratorException.class, () -> runtimeMigrator.migrate());
    assertEquals(caughtException, migratorException.getCause());
    assertTrue(migratorException.getMessage().contains("Error while fetching instances to migrate"));
    verifyNoMoreInteractions(runtimeService);
    verifyNoInteractions(camundaClient);
  }

  @Test
  public void shouldMigrationExceptionBeThrownOnCamundaClientException() {
    // given exception is thrown by C8 api
    mockProcessInstanceQueryCalls();
    ClientException caughtException = new ClientException("Process engine error");
    when(idKeyMapper.findLatestIdByType(anyString())).thenReturn("id");
    when(camundaClient.newCreateInstanceCommand()).thenThrow(caughtException);

    // when migrating, then exception is wrapped and migration halted
    MigratorException migratorException = assertThrows(MigratorException.class, () -> runtimeMigrator.migrate());
    assertEquals(caughtException, migratorException.getCause());
    assertTrue(migratorException.getMessage().contains("Error while migrating process instance"));
    verifyNoMoreInteractions(camundaClient);
  }

  private void mockProcessInstanceQueryCalls() {
    when(idKeyMapper.findLatestIdByType(anyString())).thenReturn("latestId");
    when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);

    when(processInstanceQuery.idAfter(anyString())).thenReturn(processInstanceQuery);
    when(processInstanceQuery.rootProcessInstances()).thenReturn(processInstanceQuery);
    when(processInstanceQuery.orderByProcessInstanceId()).thenReturn(processInstanceQuery);
    when(processInstanceQuery.asc()).thenReturn(processInstanceQuery);
    when(processInstanceQuery.processInstanceId(anyString())).thenReturn(processInstanceQuery);
    ExecutionEntity executionEntity = new ExecutionEntity();
    executionEntity.setId("id");
    when(processInstanceQuery.list()).thenReturn(Collections.singletonList(executionEntity));
    when(processInstanceQuery.singleResult()).thenReturn(executionEntity);

    VariableInstanceQueryImpl variableInstanceQuery = mock(VariableInstanceQueryImpl.class);
    when(runtimeService.createVariableInstanceQuery()).thenReturn(variableInstanceQuery);
    when(variableInstanceQuery.activityInstanceIdIn(anyString())).thenReturn(variableInstanceQuery);
    when(variableInstanceQuery.list()).thenReturn(Collections.emptyList());
  }

}