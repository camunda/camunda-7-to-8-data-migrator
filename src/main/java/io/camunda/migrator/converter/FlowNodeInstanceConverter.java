package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertDate;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;

@Component
public class FlowNodeInstanceConverter {

  public FlowNodeInstanceDbModel apply(HistoricActivityInstance flowNode) {
    Long key = convertIdToKey(convertActivityInstanceIdToKey(flowNode.getId()));
    return new FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder()
        .flowNodeInstanceKey(key)
        .processInstanceKey(convertIdToKey(flowNode.getProcessInstanceId()))
        .processDefinitionKey(convertIdToKey(convertProcessDefinitionIdToKey(flowNode.getProcessDefinitionId())))
        .processDefinitionId(flowNode.getProcessDefinitionKey())
        .startDate(convertDate(flowNode.getStartTime()))
        .endDate(convertDate(flowNode.getEndTime()))
        .flowNodeId(flowNode.getActivityId())
        .type(convertType(flowNode.getActivityType()))
        .tenantId(flowNode.getTenantId())
        .treePath(null) // TODO doesn't exist
        .state(null) // TODO doesn't exist
        .incidentKey(null) // TODO doesn't exist
        .numSubprocessIncidents(null) // TODO doesn't exist
        .build();
  }

  protected FlowNodeType convertType(String activityType) {
    return  switch (activityType) {
      case ActivityTypes.START_EVENT -> FlowNodeType.START_EVENT;
      case ActivityTypes.END_EVENT_NONE -> FlowNodeType.END_EVENT;
      case ActivityTypes.TASK_SERVICE -> FlowNodeType.SERVICE_TASK;
      case ActivityTypes.TASK_USER_TASK -> FlowNodeType.USER_TASK;
      default -> throw new IllegalArgumentException("Unknown type: " + activityType);
    };
  }

  protected String convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return processDefinitionId.split(":")[2];
  }

}
