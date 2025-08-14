/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.persistence;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IdKeyMapper {

  enum TYPE {
    HISTORY_PROCESS_DEFINITION("Historic Process Definition"),
    HISTORY_PROCESS_INSTANCE("Historic Process Instance"),
    HISTORY_INCIDENT("Historic Incident"),
    HISTORY_VARIABLE("Historic Variable"),
    HISTORY_USER_TASK("Historic User Task"),
    HISTORY_FLOW_NODE("Historic Flow Node"),
    HISTORY_DECISION_INSTANCE("Historic Decision Instance"),
    HISTORY_DECISION_DEFINITION("Historic Decision Definition"),
    HISTORY_DECISION_REQUIREMENTS("Historic Decision Requirements"),

    RUNTIME_PROCESS_INSTANCE("Process Instance");

    private final String displayName;

    TYPE(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  String findLatestIdByType(TYPE type);

  boolean checkExistsByIdAndType(@Param("type") TYPE type, @Param("id") String id);

  boolean checkHasKeyByIdAndType(@Param("type") TYPE type, @Param("id") String id);

  Date findLatestStartDateByType(TYPE type);

  Long findKeyById(String id);

  void insert(IdKeyDbModel idKeyDbModel);

  List<IdKeyDbModel> findSkippedByType(@Param("type") TYPE type, @Param("offset") int offset, @Param("limit") int limit);

  List<IdKeyDbModel> findMigratedByType(@Param("type") TYPE type, @Param("offset") int offset, @Param("limit") int limit);

  long countSkippedByType(@Param("type") TYPE type);

  List<String> findAllIds();

  void updateKeyById(IdKeyDbModel idKeyDbModel);

  void delete(String id);
}
