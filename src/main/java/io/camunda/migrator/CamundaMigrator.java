package io.camunda.migrator;

import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//@Component
public class CamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMigrator.class);

  private final ProcessEngine processEngine;
  private final ProcessInstanceMapper processInstanceMapper;

  public CamundaMigrator(
      ProcessEngine processEngine,
      ProcessInstanceMapper processInstanceMapper
  ) {
    this.processEngine = processEngine;
    this.processInstanceMapper = processInstanceMapper;
  }

  @PostConstruct
  public void migrate() {
    LOGGER.info("Migrating process instances");

    HistoryService historyService = processEngine.getHistoryService();
    List<HistoricProcessInstance> allHistoricProcessInstances = historyService.createHistoricProcessInstanceQuery()
        .list();

    LOGGER.info("Retrieved {} historic process instances from C7 Process Engine", allHistoricProcessInstances.size());

    processInstanceMapper.insert(new ProcessInstanceDbModel(
        1L,
        "123",
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
    ));
  }

}
