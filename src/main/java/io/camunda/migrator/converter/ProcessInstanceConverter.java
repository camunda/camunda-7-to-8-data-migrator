package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

import static io.camunda.migrator.ConverterUtil.convertIdToKey;
import static io.camunda.search.entities.ProcessInstanceEntity.ProcessInstanceState;

@Component
public class ProcessInstanceConverter {

  public ProcessInstanceDbModel apply(HistoricProcessInstance historicProcessInstance) {

    //TODO map fields of historic process instance to ProcessInstanceDbModel

    return new ProcessInstanceDbModel(
        convertIdToKey(historicProcessInstance.getId()),
        historicProcessInstance.getProcessDefinitionKey(), // TODO is this the same field?
        convertIdToKey(convertProcessDefinitionIdToKey(historicProcessInstance.getProcessDefinitionId())),
        convertState(historicProcessInstance.getState()),
        historicProcessInstance.getStartTime().toInstant().atOffset(ZoneOffset.UTC),
        historicProcessInstance.getEndTime().toInstant().atOffset(ZoneOffset.UTC),
        historicProcessInstance.getTenantId(),
        convertIdToKey(historicProcessInstance.getSuperProcessInstanceId()), // TODO is this the same field?
        null,
        null,
        null,
        historicProcessInstance.getProcessDefinitionVersion() // TODO is this the same field?
    );
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
