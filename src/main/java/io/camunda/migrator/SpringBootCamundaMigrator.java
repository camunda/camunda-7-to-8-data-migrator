package io.camunda.migrator;

import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.migrator.converter.HistoricProcessInstanceConverter;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpringBootCamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootCamundaMigrator.class);

  private final ProcessInstanceMapper processInstanceMapper;
  private final RuntimeService runtimeService;
  private final HistoryService historyService;
  private final HistoricProcessInstanceConverter converter;

  public SpringBootCamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      RuntimeService runtimeService,
      HistoryService historyService,
      HistoricProcessInstanceConverter converter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.converter = converter;
  }

  @PostConstruct
  public void migrateAllHistoricProcessInstances() {
    LOGGER.info("Migrating process instances");

    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    var allHistoricProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // Migrate process instance
    allHistoricProcessInstances.forEach(instance -> {
      ProcessInstanceDbModel dbModel = converter.apply(instance);
      processInstanceMapper.insert(dbModel);
    });
  }



}
