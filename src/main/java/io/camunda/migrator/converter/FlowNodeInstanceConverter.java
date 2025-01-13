package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;

@Component
public class FlowNodeInstanceConverter {

  public FlowNodeInstanceDbModel apply(HistoricActivityInstance historicActivityInstance) {
    Long key = convertIdToKey(convertActivityInstanceIdToKey(historicActivityInstance.getId()));
    return new FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder()
        .flowNodeInstanceKey(key)
        .processInstanceKey(convertIdToKey(historicActivityInstance.getProcessInstanceId()))
        .processDefinitionKey(convertIdToKey(convertProcessDefinitionIdToKey(historicActivityInstance.getProcessDefinitionId())))
        .processDefinitionId(historicActivityInstance.getProcessDefinitionKey())
        .startDate(historicActivityInstance.getStartTime().toInstant().atOffset(ZoneOffset.UTC))
        .endDate(historicActivityInstance.getEndTime().toInstant().atOffset(ZoneOffset.UTC))
        .flowNodeId(historicActivityInstance.getActivityId())
        .type(convertType(historicActivityInstance.getActivityType()))
        .treePath(null) // TODO doesn't exist
        .state(null) // TODO doesn't exist
        .incidentKey(null) // TODO doesn't exist
        .numSubprocessIncidents(null) // TODO doesn't exist
        .tenantId(historicActivityInstance.getTenantId())
        .build();
  }

  private FlowNodeInstanceEntity.FlowNodeType convertType(String activityType) {
    return  switch (activityType) {
      case ActivityTypes.START_EVENT -> FlowNodeInstanceEntity.FlowNodeType.START_EVENT;
      case ActivityTypes.END_EVENT_NONE -> FlowNodeInstanceEntity.FlowNodeType.END_EVENT;
      case ActivityTypes.TASK_SERVICE -> FlowNodeInstanceEntity.FlowNodeType.SERVICE_TASK;
      case ActivityTypes.TASK_USER_TASK -> FlowNodeInstanceEntity.FlowNodeType.USER_TASK;
      default -> throw new IllegalArgumentException("Unknown type: " + activityType);
    };
  }

  private String convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return processDefinitionId.split(":")[2];
  }

}
