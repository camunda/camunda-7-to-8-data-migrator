/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.clients;

import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_FIND_KEY_BY_ID;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_FIND_LATEST_ID;
import static io.camunda.migrator.impl.util.ExceptionUtils.callApi;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_CHECK_EXISTENCE;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_FIND_LATEST_START_DATE;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_FIND_SKIPPED_COUNT;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_INSERT_RECORD;
import static io.camunda.migrator.impl.logging.DbClientLogs.FAILED_TO_UPDATE_KEY;
import static io.camunda.migrator.impl.persistence.IdKeyMapper.TYPE;

import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.impl.Pagination;
import io.camunda.migrator.impl.util.PrintUtils;
import io.camunda.migrator.impl.logging.DbClientLogs;
import io.camunda.migrator.impl.persistence.IdKeyDbModel;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Wrapper class for IdKeyMapper database operations with exception handling.
 * Maintains the same exception wrapping behavior as ExceptionUtils.callApi.
 */
@Component
public class DbClient {

  @Autowired
  protected MigratorProperties properties;

  @Autowired
  protected IdKeyMapper idKeyMapper;

  /**
   * Checks if a process instance exists in the mapping table.
   */
  public boolean checkExists(String legacyEntityId) {
    return callApi(() -> idKeyMapper.checkExists(legacyEntityId), FAILED_TO_CHECK_EXISTENCE + legacyEntityId);
  }

  /**
   * Finds the latest start date by type.
   */
  public Date findLatestStartDateByType(TYPE type) {
    return callApi(() -> idKeyMapper.findLatestStartDateByType(type), FAILED_TO_FIND_LATEST_START_DATE + type);
  }

  /**
   * Finds the latest legacy ID by type.
   */
  public String findLatestIdByType(TYPE type){
    return callApi(() -> idKeyMapper.findLatestIdByType(type), FAILED_TO_FIND_LATEST_ID + type);
  }

  /**
   * Finds the key by legacy ID.
   */
  public Long findKeyById(String legacyId){
    return callApi(() -> idKeyMapper.findKeyById(legacyId), FAILED_TO_FIND_KEY_BY_ID + legacyId);
  }

  /**
   * Updates a record by setting the key for an existing ID.
   */
  public void updateKeyById(String legacyId, Long entityKey, TYPE type) {
    DbClientLogs.updatingKeyForLegacyId(legacyId, entityKey);
    var model = createIdKeyDbModel(legacyId, null, entityKey, type);
    callApi(() -> idKeyMapper.updateKeyById(model), FAILED_TO_UPDATE_KEY + entityKey);
  }

  /**
   * Inserts a new process instance record into the mapping table.
   */
  public void insert(String legacyId, Date startDate, Long entityKey, TYPE type) {
    DbClientLogs.insertingRecord(legacyId, startDate, entityKey);
    var model = createIdKeyDbModel(legacyId, startDate, entityKey, type);
    callApi(() -> idKeyMapper.insert(model), FAILED_TO_INSERT_RECORD + legacyId);
  }


  /**
   * Lists skipped process instances with pagination and prints them.
   */
  public void listSkippedProcessInstances() {
  new Pagination<String>()
      .pageSize(properties.getPageSize())
      .maxCount(idKeyMapper::findSkippedCount)
      .page(offset -> idKeyMapper.findSkipped(offset, properties.getPageSize())
          .stream()
          .map(IdKeyDbModel::id)
          .collect(Collectors.toList()))
      .callback(PrintUtils::print);
  }

  /**
   * Processes skipped process instances with pagination.
   */
  public void fetch(Consumer<IdKeyDbModel> callback) {
    new Pagination<IdKeyDbModel>()
        .pageSize(properties.getPageSize())
        .maxCount(idKeyMapper::findSkippedCount)
        // Hardcode offset to 0 since each callback updates the database and leads to fresh results.
        .page(offset -> idKeyMapper.findSkipped(0, properties.getPageSize()))
        .callback(callback);
  }

  /**
   * Finds the count of skipped process instances.
   */
  public Long findSkippedCount() {
    return callApi(idKeyMapper::findSkippedCount, FAILED_TO_FIND_SKIPPED_COUNT);
  }

  protected IdKeyDbModel createIdKeyDbModel(String id, Date startDate, Long key, TYPE type) {
    var keyIdDbModel = new IdKeyDbModel();
    keyIdDbModel.setId(id);
    keyIdDbModel.setStartDate(startDate);
    keyIdDbModel.setInstanceKey(key);
    keyIdDbModel.setType(type);
    return keyIdDbModel;
  }

}
