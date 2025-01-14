package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertDate;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;
import static io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState;

@Component
public class ProcessInstanceConverter {

  public ProcessInstanceDbModel apply(HistoricProcessInstance processInstance) {
    Long key = convertIdToKey(convertActivityInstanceIdToKey(processInstance.getId()));
    return new ProcessInstanceDbModel.ProcessInstanceDbModelBuilder()
        .processInstanceKey(key)
        .processDefinitionKey(convertIdToKey(convertProcessDefinitionIdToKey(processInstance.getProcessDefinitionId())))
        .processDefinitionId(processInstance.getProcessDefinitionKey())
        .startDate(convertDate(processInstance.getStartTime()))
        .endDate(convertDate(processInstance.getEndTime()))
        .state(convertState(processInstance.getState()))
        .tenantId(processInstance.getTenantId())
        .parentProcessInstanceKey(null) // TODO
        .parentElementInstanceKey(null) // TODO
        .numIncidents(0) // TODO
        .version(processInstance.getProcessDefinitionVersion()) // TODO
        .build();
  }

  private ProcessInstanceState convertState(String state) {
    return switch (state) {
      case "ACTIVE", "SUSPENDED" -> ProcessInstanceState.ACTIVE;
      case "COMPLETED" -> ProcessInstanceState.COMPLETED;
      case "EXTERNALLY_TERMINATED", "INTERNALLY_TERMINATED" -> ProcessInstanceState.CANCELED;

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }


  private String convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return processDefinitionId.split(":")[2];
  }
}
