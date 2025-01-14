package io.camunda.migrator;

import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.migrator.converter.FlowNodeInstanceConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.migrator.converter.UserTaskConverter;
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
  private final UserTaskMapper userTaskMapper;

  private final RuntimeService runtimeService;
  private final HistoryService historyService;

  private final ProcessInstanceConverter processInstanceConverter;
  private final FlowNodeInstanceConverter flowNodeInstanceConverter;
  private final UserTaskConverter userTaskConverter;

  public CamundaMigrator(
      ProcessInstanceMapper processInstanceMapper,
      FlowNodeInstanceMapper flowNodeInstanceMapper,
      UserTaskMapper userTaskMapper,

      RuntimeService runtimeService,
      HistoryService historyService,

      ProcessInstanceConverter processInstanceConverter,
      FlowNodeInstanceConverter flowNodeInstanceConverter,
      UserTaskConverter userTaskConverter
  ) {
    this.processInstanceMapper = processInstanceMapper;
    this.flowNodeInstanceMapper = flowNodeInstanceMapper;
    this.userTaskMapper = userTaskMapper;
    this.runtimeService = runtimeService;
    this.historyService = historyService;
    this.processInstanceConverter = processInstanceConverter;
    this.flowNodeInstanceConverter = flowNodeInstanceConverter;
    this.userTaskConverter = userTaskConverter;
  }

  @PostConstruct
  public void migrateAllHistoricProcessInstances() {
    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");
    runtimeService.startProcessInstanceByKey("simple-process-user-task");

    migrateProcessInstances();
    migrateFlowNodeInstances();
    migrateUserTasks();
  }

  private void migrateUserTasks() {
    LOGGER.info("Migrating user tasks");

    List<UserTaskDbModel> userTasks = userTaskMapper.search(null);
    List<Long> migratedKeys = userTasks.stream().map(UserTaskDbModel::userTaskKey).toList();

    historyService.createHistoricTaskInstanceQuery().list().forEach(historicTask -> {
      Long key = convertIdToKey(historicTask.getId());
      if (!migratedKeys.contains(key)) {
        UserTaskDbModel dbModel = userTaskConverter.apply(historicTask);
        userTaskMapper.insert(dbModel);
      }
    });
  }

  private void migrateFlowNodeInstances() {
    LOGGER.info("Migrating flow node instances ");

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
    LOGGER.info("Migrating process instances");

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
