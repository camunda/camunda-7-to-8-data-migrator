package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.VariableDbModel;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;

@Component
public class VariableConverter {

  public VariableDbModel apply(HistoricVariableInstance historicVariable) {

    Long processInstanceKey = convertIdToKey(historicVariable.getProcessInstanceId());
    Long key = convertIdToKey(historicVariable.getId());

    Long activityInstanceKey = convertActivityInstanceIdToKey(historicVariable.getActivityInstanceId());

    return new VariableDbModel.VariableDbModelBuilder()
        .variableKey(key)
        .name(historicVariable.getName())
        .value(getValueAsString(historicVariable)) //TODO ?
        .scopeKey(activityInstanceKey) //TODO ?
        .processInstanceKey(processInstanceKey)
        .processDefinitionId(historicVariable.getProcessDefinitionKey())
        .tenantId(historicVariable.getTenantId())
        .build();
  }

  private String getValueAsString(HistoricVariableInstance historicVariable) {
    Object value = historicVariable.getValue();
    return value != null ? value.toString() : null;
  }

}
