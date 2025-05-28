/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.history;

import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface IdKeyMapper {

  enum TYPE {
    HISTORY_PROCESS_DEFINITION,
    HISTORY_PROCESS_INSTANCE,
    HISTORY_INCIDENT,
    HISTORY_VARIABLE,
    HISTORY_USER_TASK,
    HISTORY_FLOW_NODE,
    HISTORY_DECISION_INSTANCE,

    RUNTIME_PROCESS_INSTANCE
  }

  String findLatestIdByType(TYPE type);

  Long findKeyById(String id);

  void insert(IdKeyDbModel idKeyDbModel);

  Set<String> findSkippedProcessInstanceIds(@Param("offset") int offset, @Param("limit") int limit);

  Set<String> findSkippedProcessInstanceIds();

  long findSkippedProcessInstanceIdsCount();

  Set<String> findAllProcessInstanceIds();

  void updateKeyById(IdKeyDbModel idKeyDbModel);

  void delete(String id);

}