package io.camunda.migrator;

import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

public class ProcessInstanceDbModelFactory {

  public static ProcessInstanceDbModel create(HistoricProcessInstance historicProcessInstance) {

    //TODO map fields of historic process instance to ProcessInstanceDbModel


    return new ProcessInstanceDbModel(
        1L,
        historicProcessInstance.getProcessDefinitionId(),
        1L,
        null, // processInstanceState
        null, // offsetDateTime startDate
        null, // offsetDateTime endDate
        "tenantId",
        1L, // parentProcessInstanceKey
        1L, // parentElementInstanceKey
        1,  //numIncidents
        "elementId",
        1 // version
    );
  }
}
