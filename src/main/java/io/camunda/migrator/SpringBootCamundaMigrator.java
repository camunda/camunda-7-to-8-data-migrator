package io.camunda.migrator;

import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.search.entities.ProcessInstanceEntity;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class SpringBootCamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootCamundaMigrator.class);

  @Autowired
  public ProcessInstanceMapper processInstanceMapper;

  @Autowired
  public RuntimeService runtimeService;

  @Autowired
  public HistoryService historyService;

  @Bean
  public String migrate() {
    LOGGER.info("Migrating process instances");

    // Start process instance
    runtimeService.startProcessInstanceByKey("simple-process-service-task");

    // Migrate process instance
    historyService.createHistoricProcessInstanceQuery().list().forEach(historicProcessInstance -> {
      processInstanceMapper.insert(
          new ProcessInstanceDbModel(
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
          ));
    });

    return "";
  }

  private String convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return processDefinitionId.split(":")[2];
  }

  private ProcessInstanceEntity.ProcessInstanceState convertState(String state) {
    return switch (state) {
      case "ACTIVE", "SUSPENDED" -> ProcessInstanceEntity.ProcessInstanceState.ACTIVE;
      case "COMPLETED" -> ProcessInstanceEntity.ProcessInstanceState.COMPLETED;
      case "EXTERNALLY_TERMINATED", "INTERNALLY_TERMINATED" ->
          ProcessInstanceEntity.ProcessInstanceState.CANCELED;
      default -> throw new IllegalArgumentException("Unknown state: " + state);
    };
  }

  protected Long convertIdToKey(String id) {
    // The C7 ID is UUID whereas C8 IDs are called keys.
    // C8 keys are a composite of the partition and the id.
    // TODO: convert C7 IDs correctly to C8 IDs.
    if (id == null) return null;
    return Long.valueOf(id);
  }

}
