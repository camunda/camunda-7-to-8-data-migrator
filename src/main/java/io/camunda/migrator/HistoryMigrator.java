package io.camunda.migrator;

import io.camunda.db.rdbms.read.domain.DecisionDefinitionDbQuery;
import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.IncidentDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessDefinitionDbQuery;
import io.camunda.db.rdbms.read.domain.ProcessInstanceDbQuery;
import io.camunda.db.rdbms.read.domain.UserTaskDbQuery;
import io.camunda.db.rdbms.read.domain.VariableDbQuery;
import io.camunda.db.rdbms.sql.DecisionDefinitionMapper;
import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper;
import io.camunda.db.rdbms.sql.IncidentMapper;
import io.camunda.db.rdbms.sql.ProcessDefinitionMapper;
import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.write.domain.DecisionDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.migrator.converter.DecisionDefinitionConverter;
import io.camunda.migrator.converter.FlowNodeConverter;
import io.camunda.migrator.converter.IncidentConverter;
import io.camunda.migrator.converter.ProcessDefinitionConverter;
import io.camunda.migrator.converter.ProcessInstanceConverter;
import io.camunda.migrator.converter.UserTaskConverter;
import io.camunda.migrator.converter.VariableConverter;
import io.camunda.search.filter.FlowNodeInstanceFilter;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.HistoricActivityInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricIncidentQueryImpl;
import org.camunda.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricVariableInstanceQueryImpl;
import org.camunda.bpm.engine.impl.ProcessDefinitionQueryImpl;
import org.camunda.bpm.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryMigrator {

  protected static int BATCH_SIZE = 500;

  private static final Logger LOGGER = LoggerFactory.getLogger(HistoryMigrator.class);

  // Mappers

  @Autowired
  private ProcessInstanceMapper processInstanceMapper;

  @Autowired
  private UserTaskMapper userTaskMapper;

  @Autowired
  private VariableMapper variableMapper;

  @Autowired
  private IncidentMapper incidentMapper;

  @Autowired
  private ProcessDefinitionMapper processDefinitionMapper;

  @Autowired
  private DecisionDefinitionMapper decisionDefinitionMapper;

  @Autowired
  private FlowNodeInstanceMapper flowNodeMapper;

  // Services

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private HistoryService historyService;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private ManagementService managementService;

  // Converters

  @Autowired
  private ProcessInstanceConverter processInstanceConverter;

  @Autowired
  private FlowNodeConverter flowNodeConverter;

  @Autowired
  private UserTaskConverter userTaskConverter;

  @Autowired
  private VariableConverter variableConverter;

  @Autowired
  private IncidentConverter incidentConverter;

  @Autowired
  private ProcessDefinitionConverter processDefinitionConverter;

  @Autowired
  private DecisionDefinitionConverter decisionDefinitionConverter;

  public void migrate() {
    LOGGER.info("Migrating C7 data...");
    // Start process instance
    //String processInstanceId = runtimeService.startProcessInstanceByKey("fill_all_tabs", Variables.putValue("targetValue", 5_000_000)).getId();

    //executeAllJobs(processInstanceId);

    migrateProcessDefinitions();
    migrateProcessInstances();
    migrateFlowNodes();
    migrateUserTasks();
    migrateVariables();
    migrateIncidents();

    //migrateDecisionDefinitions();
  }

  protected void executeAllJobs(String processInstanceId) {
    String nextJobId = getNextExecutableJobId(processInstanceId);

    while (nextJobId != null) {
      try {
        managementService.executeJob(nextJobId);
      } catch (Throwable t) { /* ignore */
      }
      nextJobId = getNextExecutableJobId(processInstanceId);
    }

  }

  protected String getNextExecutableJobId(String processInstanceId) {
    List<Job> jobs = managementService.createJobQuery()
        .processInstanceId(processInstanceId)
        .executable()
        .listPage(0, 1);
    if (jobs.size() == 1) {
      return jobs.get(0).getId();
    } else {
      return null;
    }
  }

  private void migrateDecisionDefinitions() {
    repositoryService.createDecisionDefinitionQuery().list().forEach(legacyDecisionDefinition -> {
      String decisionDefinitionId = legacyDecisionDefinition.getId();

      if (checkDecisionDefinitionNotMigrated(decisionDefinitionId)) {
        LOGGER.info("Migration of legacy decision definition with id '{}' completed", decisionDefinitionId);
        DecisionDefinitionDbModel dbModel = decisionDefinitionConverter.apply(legacyDecisionDefinition);
        decisionDefinitionMapper.insert(dbModel);
      } else {
        LOGGER.info("Legacy decision definition with id '{}' has been migrated already. Skipping.",
            decisionDefinitionId);
      }
    });
  }

  private void migrateProcessDefinitions() {
    // 1. SELECT auf C8 PROCESS_INSTANCE table sort DESC by legacy ID and get element at index 0
    //    LIMIT 1
    // If empty, start without filter.
    // 2. Pass legacy UUID into API query criterion "return bigger than UUID".
    //    SELECT LIMIT 500;
    // 3. Migrate the result
    // 4. When migration successful use last element from returned PIs and pass again to #2.

    ProcessDefinitionQueryImpl legacyProcessDefinitionQuery = (ProcessDefinitionQueryImpl) repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionId()
        .asc();

    String latestLegacyId = processDefinitionMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyProcessDefinitionQuery.idAfter(latestLegacyId);
    }

    long maxLegacyProcessDefinitionsCount = legacyProcessDefinitionQuery.count();
    for (int i = 0; i < maxLegacyProcessDefinitionsCount; i = i + BATCH_SIZE - 1) {
      legacyProcessDefinitionQuery.listPage(i, BATCH_SIZE).forEach(legacyProcessDefinition -> {
        String processDefinitionId = legacyProcessDefinition.getId();
        if (checkProcessDefinitionNotMigrated(processDefinitionId)) {
          LOGGER.info("Migration of legacy process definition with id '{}' completed", processDefinitionId);
          ProcessDefinitionDbModel dbModel = processDefinitionConverter.apply(legacyProcessDefinition);
          processDefinitionMapper.insert(dbModel);
        } else {
          LOGGER.info("Legacy process definition with id '{}' has been migrated already. Skipping.",
              processDefinitionId);
        }
      });
    }
  }

  private void migrateProcessInstances() {
    HistoricProcessInstanceQueryImpl legacyProcessInstanceQuery = (HistoricProcessInstanceQueryImpl) historyService.createHistoricProcessInstanceQuery()
        .orderByProcessInstanceId()
        .asc();

    String latestLegacyId = processInstanceMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyProcessInstanceQuery.idAfter(latestLegacyId);
    }

    long maxLegacyProcessInstancesCount = legacyProcessInstanceQuery.count();
    for (int i = 0; i < maxLegacyProcessInstancesCount; i = i + BATCH_SIZE - 1) {
      legacyProcessInstanceQuery.listPage(i, BATCH_SIZE).forEach(legacyProcessInstance -> {
        String legacyProcessInstanceId = legacyProcessInstance.getId();
        if (checkProcessInstanceNotMigrated(legacyProcessInstanceId)) {

          Long processDefinitionKey = findProcessDefinitionKey(legacyProcessInstance.getProcessDefinitionId());
          if (processDefinitionKey != null) {
            String legacySuperProcessInstanceId = legacyProcessInstance.getSuperProcessInstanceId();
            Long parentProcessInstanceKey = null;
            if (legacySuperProcessInstanceId != null) {
              parentProcessInstanceKey = findProcessInstanceKey(legacySuperProcessInstanceId).processInstanceKey();
            }

            if (parentProcessInstanceKey != null
                // Continue if PI has no parent.
                || legacySuperProcessInstanceId == null) {
              LOGGER.info("Migration of legacy process instances with id '{}' completed", legacyProcessInstanceId);
              ProcessInstanceDbModel dbModel = processInstanceConverter.apply(legacyProcessInstance,
                  processDefinitionKey, parentProcessInstanceKey);
              processInstanceMapper.insert(dbModel);
            } else {
              LOGGER.info(
                  "Migration of legacy process instance with id '{}' skipped. Parent process instance not yet available.",
                  legacyProcessInstanceId);
            }
          } else {
            LOGGER.info(
                "Migration of legacy process instance with id '{}' skipped. Process definition not yet available.",
                legacyProcessInstanceId);
          }
        } else {
          LOGGER.info("Legacy process instances with id '{}' has been migrated already. Skipping.",
              legacyProcessInstanceId);
        }
      });
    }
  }

  private void migrateIncidents() {
    HistoricIncidentQueryImpl legacyIncidentQuery = (HistoricIncidentQueryImpl) historyService.createHistoricIncidentQuery()
        .orderByIncidentId()
        .asc();

    String latestLegacyId = incidentMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyIncidentQuery.idAfter(latestLegacyId);
    }

    long maxLegacyIncidentsCount = legacyIncidentQuery.count();
    for (int i = 0; i < maxLegacyIncidentsCount; i = i + BATCH_SIZE - 1) {
      legacyIncidentQuery.listPage(i, BATCH_SIZE).forEach(legacyIncident -> {
        String legacyIncidentId = legacyIncident.getId();
        if (checkIncidentNotMigrated(legacyIncidentId)) {
          ProcessInstanceDbModel legacyProcessInstance = findProcessInstanceKey(legacyIncident.getProcessInstanceId());
          if (legacyProcessInstance != null) {
            Long processInstanceKey = legacyProcessInstance.processInstanceKey();
            if (processInstanceKey != null) {
              Long flowNodeInstanceKey = findFlowNodeKey(legacyIncident.getActivityId(), legacyIncident.getProcessInstanceId());
              LOGGER.info("Migration of legacy incident with id '{}' completed.", legacyIncidentId);
              Long processDefinitionKey = findProcessDefinitionKey(legacyIncident.getProcessDefinitionId());
              Long jobDefinitionKey = null; // TODO Job table doesn't exist yet.
              IncidentDbModel dbModel = incidentConverter.apply(legacyIncident, processDefinitionKey,
                  processInstanceKey, jobDefinitionKey, flowNodeInstanceKey);
              incidentMapper.insert(dbModel);
            } else {
              LOGGER.info("Migration of legacy incident with id '{}' skipped. Process instance not yet available.", legacyIncidentId);
            }
          }
        } else {
          LOGGER.info("Legacy incident with id '{}' has been migrated already. Skipping.", legacyIncidentId);
        }
      });
    }
  }

  private void migrateVariables() {
    HistoricVariableInstanceQueryImpl legacyVariableQuery = (HistoricVariableInstanceQueryImpl) historyService.createHistoricVariableInstanceQuery().orderByVariableId().asc();

    String latestLegacyId = variableMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyVariableQuery.idAfter(latestLegacyId);
    }

    long maxLegacyVariablesCount = legacyVariableQuery.count();
    for (int i = 0; i < maxLegacyVariablesCount; i = i + BATCH_SIZE - 1) {
      legacyVariableQuery.listPage(i, BATCH_SIZE).forEach(legacyVariable -> {
        String legacyVariableId = legacyVariable.getId();
        if (checkVariableNotMigrated(legacyVariableId)) {
          String legacyProcessInstanceId = legacyVariable.getProcessInstanceId();
          ProcessInstanceDbModel processInstance = findProcessInstanceKey(legacyProcessInstanceId);
          if (processInstance != null) {
            Long processInstanceKey = processInstance.processInstanceKey();
            Long scopeKey = findFlowNodeKey(legacyVariable.getActivityInstanceId()); // TODO does this cover scope correctly?
            if (scopeKey != null) {
              LOGGER.info("Migration of legacy variable with id '{}' completed.", legacyVariableId);
              VariableDbModel dbModel = variableConverter.apply(legacyVariable, processInstanceKey, scopeKey);
              variableMapper.insert(dbModel);
            } else {
              LOGGER.info("Migration of legacy variable with id '{}' skipped. Activity instance not yet available.",
                  legacyVariableId);
            }
          } else {
            LOGGER.info("Migration of legacy variable with id '{}' skipped. Process instance not yet available.",
                legacyVariableId);
          }
        } else {
          LOGGER.info("Legacy variable with id '{}' has been migrated already. Skipping.", legacyVariableId);
        }
      });
    }
  }

  private void migrateUserTasks() {
    HistoricTaskInstanceQueryImpl legacyTaskQuery = (HistoricTaskInstanceQueryImpl) historyService.createHistoricTaskInstanceQuery()
        .orderByTaskId()
        .asc();

    String latestLegacyId = userTaskMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyTaskQuery.idAfter(latestLegacyId);
    }

    long maxLegacyTasksCount = legacyTaskQuery.count();
    for (int i = 0; i < maxLegacyTasksCount; i = i + BATCH_SIZE - 1) {
      legacyTaskQuery.listPage(i, BATCH_SIZE).forEach(legacyUserTask -> {
        String legacyUserTaskId = legacyUserTask.getId();
        if (checkUserTaskNotMigrated(legacyUserTaskId)) {
          ProcessInstanceDbModel processInstance = findProcessInstanceKey(legacyUserTask.getProcessInstanceId());
          if (processInstance != null) {
            Long elementInstanceKey = findFlowNodeKey(legacyUserTask.getActivityInstanceId());
            if (elementInstanceKey != null) {
              LOGGER.info("Migration of legacy user task with id '{}' completed.", legacyUserTaskId);
              Long processDefinitionKey = findProcessDefinitionKey(legacyUserTask.getProcessDefinitionId());
              UserTaskDbModel dbModel = userTaskConverter.apply(legacyUserTask, processDefinitionKey, processInstance,
                  elementInstanceKey);
              userTaskMapper.insert(dbModel);
            } else {
              LOGGER.info("Migration of legacy user task with id '{}' skipped. Flow node instance yet not available.",
                  legacyUserTaskId);
            }
          } else {
            LOGGER.info("Migration of legacy user task with id '{}' skipped. Process instance '{}' not yet available.",
                legacyUserTaskId, legacyUserTask.getProcessInstanceId());
          }
        } else {
          LOGGER.info("Legacy user task with id '{}' has been migrated already. Skipping.", legacyUserTaskId);
        }
      });
    }
  }

  private void migrateFlowNodes() {
    HistoricActivityInstanceQueryImpl legacyFlowNodeQuery = (HistoricActivityInstanceQueryImpl) historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().asc();

    String latestLegacyId = flowNodeMapper.findLatestId();
    if (latestLegacyId != null) {
      legacyFlowNodeQuery.idAfter(latestLegacyId);
    }

    long maxLegacyFlowNodeInstancesCount = legacyFlowNodeQuery.count();
    for (int i = 0; i < maxLegacyFlowNodeInstancesCount; i = i + BATCH_SIZE - 1) {
      legacyFlowNodeQuery.listPage(i, BATCH_SIZE).forEach(legacyFlowNode -> {
        String legacyFlowNodeId = legacyFlowNode.getId();
        if (checkFlowNodeNotMigrated(legacyFlowNodeId)) {
          ProcessInstanceDbModel legacyProcessInstance = findProcessInstanceKey(legacyFlowNode.getProcessInstanceId());
          if (legacyProcessInstance != null) {
            Long processInstanceKey = legacyProcessInstance.processInstanceKey();
            LOGGER.info("Migration of legacy flow node with id '{}' completed.", legacyFlowNodeId);
            Long processDefinitionKey = findProcessDefinitionKey(legacyFlowNode.getProcessDefinitionId());
            FlowNodeInstanceDbModel dbModel = flowNodeConverter.apply(legacyFlowNode, processDefinitionKey,
                processInstanceKey);
            flowNodeMapper.insert(dbModel);
          } else {
            LOGGER.info("Migration of legacy flow node with id '{}' skipped. Process instance not yet available.", legacyFlowNodeId);
          }
        } else {
          LOGGER.info("Legacy flow node with id '{}' has been migrated already. Skipping.", legacyFlowNodeId);
        }
      });
    }
  }

  protected ProcessInstanceDbModel findProcessInstanceKey(String processInstanceId) {
    if (processInstanceId == null)
      return null;

    List<ProcessInstanceDbModel> processInstances = processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b -> b.legacyProcessInstanceId(processInstanceId)));

    if (!processInstances.isEmpty()) {
      return processInstances.get(0);
    } else {
      return null;
    }
  }

  private Long findProcessDefinitionKey(String processDefinitionId) {
    List<ProcessDefinitionDbModel> processDefinitions = processDefinitionMapper.search(
        ProcessDefinitionDbQuery.of(b -> b.legacyId(processDefinitionId)));

    if (!processDefinitions.isEmpty()) {
      return processDefinitions.get(0).processDefinitionKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityId, String processInstanceId) {
    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(FlowNodeInstanceDbQuery.of(
        b -> b.filter(FlowNodeInstanceFilter.of(f -> f.flowNodeIds(activityId)))
            .legacyProcessInstanceId(processInstanceId)));

    if (!flowNodes.isEmpty()) {
      return flowNodes.get(0).flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  private Long findFlowNodeKey(String activityInstanceId) {
    List<FlowNodeInstanceDbModel> flowNodes = flowNodeMapper.search(
        FlowNodeInstanceDbQuery.of(b -> b.legacyId(activityInstanceId)));

    if (!flowNodes.isEmpty()) {
      return flowNodes.get(0).flowNodeInstanceKey();
    } else {
      return null;
    }
  }

  protected boolean checkProcessDefinitionNotMigrated(String legacyProcessDefinitionId) {
    return processDefinitionMapper.search(ProcessDefinitionDbQuery.of(b -> b.legacyId(legacyProcessDefinitionId)))
        .isEmpty();
  }

  protected boolean checkProcessInstanceNotMigrated(String legacyProcessInstanceId) {
    return processInstanceMapper.search(
        ProcessInstanceDbQuery.of(b -> b.legacyProcessInstanceId(legacyProcessInstanceId))).isEmpty();
  }

  protected boolean checkIncidentNotMigrated(String legacyId) {
    return incidentMapper.search(IncidentDbQuery.of(b -> b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkVariableNotMigrated(String legacyId) {
    return variableMapper.search(VariableDbQuery.of(b -> b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkUserTaskNotMigrated(String legacyId) {
    return userTaskMapper.search(UserTaskDbQuery.of(b -> b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkFlowNodeNotMigrated(String legacyId) {
    return flowNodeMapper.search(FlowNodeInstanceDbQuery.of(b -> b.legacyId(legacyId))).isEmpty();
  }

  protected boolean checkDecisionDefinitionNotMigrated(String decisionDefinitionId) {
    return decisionDefinitionMapper.search(DecisionDefinitionDbQuery.of(b -> b.legacyId(decisionDefinitionId)))
        .isEmpty();
  }

}
