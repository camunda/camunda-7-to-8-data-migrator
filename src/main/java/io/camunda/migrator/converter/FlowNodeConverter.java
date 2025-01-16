package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

import static io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder;
import static io.camunda.migrator.ConverterUtil.convertDate;
import static io.camunda.migrator.ConverterUtil.getNextKey;
import static io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;

@Component
public class FlowNodeConverter {

  public FlowNodeInstanceDbModel apply(HistoricActivityInstance flowNode,
                                       Long processDefinitionKey,
                                       Long processInstanceKey) {
    return new FlowNodeInstanceDbModelBuilder()
        .flowNodeInstanceKey(getNextKey())

        .legacyId(flowNode.getId())
        .legacyProcessInstanceId(flowNode.getProcessInstanceId())

        .flowNodeId(flowNode.getActivityId())
        .processInstanceKey(processInstanceKey)
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(flowNode.getProcessDefinitionKey())
        .startDate(convertDate(flowNode.getStartTime()))
        .endDate(convertDate(flowNode.getEndTime()))
        .type(convertType(flowNode.getActivityType()))
        .tenantId(flowNode.getTenantId())
        .state(null) // TODO: Doesn't exist in C7 activity instance. Inherited from process instance.
        .treePath(null) // TODO: Doesn't exist in C7 activity instance. Not yet supported by C8 RDBMS
        .incidentKey(null) // TODO Doesn't exist in C7 activity instance.
        .numSubprocessIncidents(null) // TODO: increment/decrement when incident exist in subprocess. C8 RDBMS specific.
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

}
