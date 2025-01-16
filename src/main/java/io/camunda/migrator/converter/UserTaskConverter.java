package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.convertDate;
import static io.camunda.migrator.ConverterUtil.getNextKey;

@Component
public class UserTaskConverter {

  public UserTaskDbModel apply(HistoricTaskInstance historicTask,
                               Long processDefinitionKey,
                               ProcessInstanceDbModel processInstance,
                               Long elementInstanceKey) {

    return new UserTaskDbModel.Builder()
        .userTaskKey(getNextKey())

        .legacyId(historicTask.getId())
        .legacyProcessInstanceId(historicTask.getProcessInstanceId())

        .elementId(historicTask.getTaskDefinitionKey())
        .processDefinitionId(historicTask.getProcessDefinitionKey())
        .creationDate(convertDate(historicTask.getStartTime()))
        .completionDate(convertDate(historicTask.getEndTime()))
        .assignee(historicTask.getAssignee())
        .state(convertState(historicTask.getTaskState()))
        .processDefinitionKey(processDefinitionKey)
        .processInstanceKey(processInstance.processInstanceKey())
        .tenantId(historicTask.getTenantId())
        .elementInstanceKey(elementInstanceKey)
        .dueDate(convertDate(historicTask.getDueDate()))
        .followUpDate(convertDate(historicTask.getFollowUpDate()))
        .priority(historicTask.getPriority())
        .processDefinitionVersion(processInstance.version())
        .formKey(null) //TODO ?
        .candidateGroups(null) //TODO ?
        .candidateUsers(null) //TODO ?
        .externalFormReference(null) //TODO ?
        .customHeaders(null) //TODO ?
        .build();
  }

  // See TaskEntity.TaskState
  private UserTaskDbModel.UserTaskState convertState(String state) {
    return switch (state) {
      case "Init", "Created" -> UserTaskDbModel.UserTaskState.CREATED; //TODO check correctness
      case "Completed" -> UserTaskDbModel.UserTaskState.COMPLETED;
      case "Deleted" -> UserTaskDbModel.UserTaskState.CANCELED;
      case "Updated" -> UserTaskDbModel.UserTaskState.CREATED;

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }

}
