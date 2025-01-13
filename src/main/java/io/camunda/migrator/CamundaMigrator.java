package io.camunda.migrator;

import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.migrator.converter.FlowNodeInstanceConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.ProcessInstanceEntity;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.camunda.migrator.ConverterUtil.convertActivityInstanceIdToKey;
import static io.camunda.migrator.ConverterUtil.convertIdToKey;

@Component
public class CamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMigrator.class);

  private final ProcessInstanceMapper processInstanceMapper;
  private final FlowNodeInstanceMapper flowNodeInstanceMapper;
  private final RuntimeService runtimeService;
  private final HistoryService historyService;
  private final ProcessInstanceConverter processInstanceConverter;
  private final FlowNodeInstanceConverter flowNodeInstanceConverter;

  public CamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      FlowNodeInstanceMapper flowNodeInstanceMapper,
      RuntimeService runtimeService,
      HistoryService historyService,
      ProcessInstanceConverter processInstanceConverter,
      FlowNodeInstanceConverter flowNodeInstanceConverter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.flowNodeInstanceMapper = flowNodeInstanceMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.processInstanceConverter = processInstanceConverter;
    this.flowNodeInstanceConverter = flowNodeInstanceConverter;
  }

  @PostConstruct
  public void migrateAllHistoricProcessInstances() {
    LOGGER.info("Migrating process instances");

    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    migrateProcessInstances();
    migrateFlowNodeInstances();
  }

  private void migrateFlowNodeInstances() {
    List<FlowNodeInstanceEntity> c8FlowNodeInstances = flowNodeInstanceMapper.search(null);
    List<Long> migratedKeys = c8FlowNodeInstances.stream().map(FlowNodeInstanceEntity::flowNodeInstanceKey).toList();

    historyService.createHistoricActivityInstanceQuery().list().forEach(historicActivity -> {
      Long key = convertIdToKey(convertActivityInstanceIdToKey(historicActivity.getId()));
      if (!migratedKeys.contains(key)) {
        FlowNodeInstanceDbModel dbModel = flowNodeInstanceConverter.apply(historicActivity);
        flowNodeInstanceMapper.insert(dbModel);
      }
    });
  }

  private void migrateProcessInstances() {
    List<ProcessInstanceEntity> c8ProcessInstances = processInstanceMapper.search(null);
    List<Long> migratedKeys = c8ProcessInstances.stream().map(ProcessInstanceEntity::processInstanceKey).toList();

    historyService.createHistoricProcessInstanceQuery().list().forEach(historicProcessInstance -> {
      if (!migratedKeys.contains(convertIdToKey(historicProcessInstance.getId()))) {
        ProcessInstanceDbModel dbModel = processInstanceConverter.apply(historicProcessInstance);
        processInstanceMapper.insert(dbModel);
      }
    });
  }

}
