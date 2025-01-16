package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.search.entities.IncidentEntity;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.*;

@Component
public class IncidentConverter {

  public IncidentDbModel apply(HistoricIncident historicIncident,
                               Long processDefinitionKey,
                               Long processInstanceKey,
                               Long jobDefinitionKey) {
    return new IncidentDbModel.Builder()
        .legacyId(historicIncident.getId())
        .legacyProcessInstanceId(historicIncident.getProcessInstanceId())
        .incidentKey(getNextKey())
        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(historicIncident.getProcessDefinitionKey())
        .processInstanceKey(processInstanceKey)
        .flowNodeInstanceKey(null) //TODO ?
        .flowNodeId(null) //TODO ?
        .jobKey(jobDefinitionKey)
        .errorType(convertErrorType(historicIncident.getIncidentType()))
        .errorMessage(historicIncident.getIncidentMessage())
        .creationDate(convertDate(historicIncident.getCreateTime()))
        .state(null) //TODO ?
        .treePath(null) //TODO ?
        .tenantId(historicIncident.getTenantId())
        .build();
  }

  //TODO how should the error types be mapped into c8 incident types?
  private IncidentEntity.ErrorType convertErrorType(String incidentType) {
    return switch (incidentType) {
      case "failedJob", "failedExternalTask" -> IncidentEntity.ErrorType.UNKNOWN;
      default -> throw new IllegalArgumentException("Unknown incidentType: " + incidentType);
    };
  }

}
