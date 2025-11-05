/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.interceptor;

import static io.camunda.migrator.impl.util.ConverterUtil.convertDate;
import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.constants.MigratorConstants;
import io.camunda.migrator.impl.logging.VariableConverterLogs;
import io.camunda.migrator.impl.util.ConverterUtil;
import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.variable.impl.value.NullValueImpl;
import org.camunda.bpm.engine.variable.impl.value.ObjectValueImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Built-in interceptor for converting HistoricVariableInstance to C8 VariableDbModel.
 */
@Component
public class DefaultVariableConverter implements EntityInterceptor {

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(HistoricVariableInstance.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof HistoricVariableInstance historicVariable)) {
      return;
    }

    // Extract metadata
    Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
    Long scopeKey = (Long) context.getMetadata("scopeKey");

    // TODO currently the VariableDbModelBuilder maps all variables to String type
    VariableDbModel dbModel = new VariableDbModel.VariableDbModelBuilder()
        .variableKey(getNextKey())
        .name(historicVariable.getName())
        .value(convertValue(historicVariable)) // TODO https://github.com/camunda/camunda-bpm-platform/issues/5329
        .scopeKey(scopeKey)
        .processInstanceKey(processInstanceKey)
        .processDefinitionId(historicVariable.getProcessDefinitionKey())
        .tenantId(ConverterUtil.getTenantId(historicVariable.getTenantId()))
        .partitionId(MigratorConstants.C7_HISTORY_PARTITION_ID)
        .historyCleanupDate(convertDate(historicVariable.getRemovalTime()))
        .build();

    // Set the built model in the context
    @SuppressWarnings("unchecked")
    EntityConversionContext<HistoricVariableInstance, VariableDbModel> typedContext =
        (EntityConversionContext<HistoricVariableInstance, VariableDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }

  private String convertValue(HistoricVariableInstance variable) {
    var variableId = variable.getId();

    if (isNullValueType(variable)) {
      VariableConverterLogs.convertingOfType(variableId, "NullValue");
      return null;
    }

    if (isPrimitiveType(variable)) {
      VariableConverterLogs.convertingOfType(variableId, "Primitive");
      var typedValue = variable.getTypedValue().getValue();

      return typedValue != null ? typedValue.toString() : null;
    }

    if (isObjectType(variable)) {
      ObjectValueImpl typedValue = (ObjectValueImpl) (variable.getTypedValue());
      Class<?> objectType = typedValue.getObjectType();
      VariableConverterLogs.convertingOfType(variableId, objectType.getSimpleName());

      return getJsonValue(typedValue);
    }

    VariableConverterLogs.warnNoHandlingAvailable(variableId, "unknown"/*variable.getTypeName()*/);
    return null;
  }

  private boolean isNullValueType(HistoricVariableInstance variable) {
    return variable.getTypedValue() instanceof NullValueImpl;
  }

  private boolean isObjectType(HistoricVariableInstance variable) {
    return variable.getTypedValue() instanceof ObjectValueImpl;
  }

  private boolean isPrimitiveType(HistoricVariableInstance variable) {
    return variable.getTypedValue() instanceof PrimitiveTypeValueImpl;
  }

  private String getJsonValue(ObjectValueImpl typedValue) {
    try {
      return objectMapper.writeValueAsString(typedValue.getValue());
    } catch (JsonProcessingException e) {
      VariableConverterLogs.failedConvertingJson(typedValue, e.getMessage());
      return null;
    }
  }
}

