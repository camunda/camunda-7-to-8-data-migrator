package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.convertDate;
import static io.camunda.migrator.ConverterUtil.getNextKey;

@Component
public class UserTaskConverter {

  public UserTaskDbModel apply(HistoricTaskInstance historicTask,
                               Long processDefinitionKey,
                               Long processInstanceKey) {

    return new UserTaskDbModel.Builder()
        .legacyId(historicTask.getId())
        .legacyProcessInstanceId(historicTask.getProcessInstanceId())
        .userTaskKey(getNextKey())
        .elementId(null) //TODO ?
        .processDefinitionId(historicTask.getProcessDefinitionKey())
        .creationDate(convertDate(historicTask.getStartTime()))
        .completionDate(convertDate(historicTask.getEndTime()))
        .assignee(historicTask.getAssignee())
        .state(convertState(historicTask.getTaskState()))
        .formKey(null) //TODO ?
        .processDefinitionKey(processDefinitionKey)
        .processInstanceKey(processInstanceKey)
        .elementInstanceKey(null) // TODO ?
        .tenantId(historicTask.getTenantId())
        .dueDate(convertDate(historicTask.getDueDate()))
        .followUpDate(convertDate(historicTask.getFollowUpDate()))
        .candidateGroups(null) //TODO ?
        .candidateUsers(null) //TODO ?
        .externalFormReference(null) //TODO ?
        .processDefinitionVersion(0) //TODO ?
        .customHeaders(null) //TODO ?
        .priority(historicTask.getPriority())
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
