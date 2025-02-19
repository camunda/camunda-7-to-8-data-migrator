package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.search.entities.IncidentEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.IncidentState;
import org.camunda.bpm.engine.impl.history.event.HistoricIncidentEventEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.springframework.stereotype.Component;

import static io.camunda.migrator.ConverterUtil.*;

@Component
public class IncidentConverter {

  public IncidentDbModel apply(HistoricIncident historicIncident,
                               Long processDefinitionKey,
                               Long processInstanceKey,
                               Long jobDefinitionKey,
                               Long flowNodeInstanceKey) {
    return new IncidentDbModel.Builder()
        .incidentKey(getNextKey())

        .legacyId(historicIncident.getId())
        .legacyProcessInstanceId(historicIncident.getProcessInstanceId())

        .processDefinitionKey(processDefinitionKey)
        .processDefinitionId(historicIncident.getProcessDefinitionKey())
        .processInstanceKey(processInstanceKey)
        .flowNodeInstanceKey(flowNodeInstanceKey) //TODO: is this linking correct?
        .flowNodeId(historicIncident.getActivityId())
        .jobKey(jobDefinitionKey)
        .errorType(null) // TODO: does error type exist in C7?
        .errorMessage(historicIncident.getIncidentMessage())
        .creationDate(convertDate(historicIncident.getCreateTime()))
        .state(convertState(0)) //TODO: make HistoricIncidentEventEntity#getIncidentState() accessible
        .treePath(null) //TODO ?
        .tenantId(historicIncident.getTenantId())
        .build();
  }

  private IncidentEntity.IncidentState convertState(Integer state) {
    return switch (state) {
      case 0 -> IncidentEntity.IncidentState.ACTIVE; // open
      case 1, 2 -> IncidentEntity.IncidentState.RESOLVED; // resolved/deleted

      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }

}
