/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.clients;

import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_DEPLOY_C8_RESOURCES;
import static io.camunda.migrator.impl.util.ExceptionUtils.callApi;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_CREATE_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_ACTIVATE_JOBS;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_FETCH_PROCESS_DEFINITION_XML;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_FETCH_VARIABLE;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_MODIFY_PROCESS_INSTANCE;
import static io.camunda.migrator.impl.logging.C8ClientLogs.FAILED_TO_SEARCH_PROCESS_DEFINITIONS;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.DeployResourceCommandStep1;
import io.camunda.client.api.command.ModifyProcessInstanceCommandStep1.ModifyProcessInstanceCommandStep3;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessDefinition;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.impl.model.FlowNodeActivation;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Wrapper class for Camunda 8 Client API calls with exception handling.
 * Maintains the same exception wrapping behavior as ExceptionUtils.callApi.
 */
@Component
public class C8Client {

  @Autowired
  protected MigratorProperties properties;

  @Autowired
  protected CamundaClient camundaClient;

  /**
   * Creates a new process instance with the given BPMN process ID and variables.
   */
  public ProcessInstanceEvent createProcessInstance(String bpmnProcessId, Map<String, Object> variables) {
    var createProcessInstance = camundaClient.newCreateInstanceCommand()
        .bpmnProcessId(bpmnProcessId)
        .latestVersion()
        .variables(variables);

    return callApi(createProcessInstance::execute, FAILED_TO_CREATE_PROCESS_INSTANCE + bpmnProcessId);
  }

  /**
   * Searches for process definitions with the given process definition ID.
   */
  public SearchResponse<ProcessDefinition> searchProcessDefinitions(String processDefinitionId) {
    var searchRequest = camundaClient.newProcessDefinitionSearchRequest()
        .filter(filter -> filter.processDefinitionId(processDefinitionId))
        .sort(s -> s.version().desc());

    return callApi(searchRequest::execute, FAILED_TO_SEARCH_PROCESS_DEFINITIONS + processDefinitionId);
  }

  /**
   * Gets the XML content of a process definition by its key.
   */
  public String getProcessDefinitionXml(long processDefinitionKey) {
    var getXmlRequest = camundaClient.newProcessDefinitionGetXmlRequest(processDefinitionKey);
    return callApi(getXmlRequest::execute, FAILED_TO_FETCH_PROCESS_DEFINITION_XML + processDefinitionKey);
  }

  /**
   * Activates jobs for the specified job type.
   */
  public List<ActivatedJob> activateJobs(String jobType) {
    var activateJobs = camundaClient.newActivateJobsCommand()
        .jobType(jobType)
        .maxJobsToActivate(properties.getPageSize());
    return callApi(activateJobs::execute, FAILED_TO_ACTIVATE_JOBS + jobType).getJobs();
  }

  /**
   * Gets a variable value from an activated job.
   */
  public Object getJobVariable(ActivatedJob job, String variableName) {
    return callApi(() -> job.getVariable(variableName), String.format(FAILED_TO_FETCH_VARIABLE, variableName, job.getKey()));
  }

  /**
   * Creates and executes a process instance modification with activity activation.
   */
  public void modifyProcessInstance(long processInstanceKey,
                                    long startEventInstanceKey,
                                    List<FlowNodeActivation> flowNodeActivations) {
    var modifyProcessInstance = camundaClient.newModifyProcessInstanceCommand(processInstanceKey);

    // Cancel start event instance where migrator job sits to avoid executing the activities twice.
    modifyProcessInstance.terminateElement(startEventInstanceKey);

    flowNodeActivations.forEach(flowNodeActivation -> {
      String activityId = flowNodeActivation.activityId();
      Map<String, Object> variables = flowNodeActivation.variables();
      if (variables != null && !variables.isEmpty()) {
        modifyProcessInstance.activateElement(activityId).withVariables(variables, activityId);
      } else {
        modifyProcessInstance.activateElement(activityId);
      }
    });

    callApi(() -> ((ModifyProcessInstanceCommandStep3) modifyProcessInstance).execute(), FAILED_TO_MODIFY_PROCESS_INSTANCE + processInstanceKey);
  }

  /**
   * Deploys C8 models from the given set of model files.
   */
  public void deployResources(Set<Path> models) {
    DeployResourceCommandStep1.DeployResourceCommandStep2 deployResourceCmd = null;
    var deployResource = camundaClient.newDeployResourceCommand();
    for (Path model : models) {
      deployResourceCmd = deployResource.addResourceFile(model.toString());
    }

    if (deployResourceCmd != null) {
      callApi(deployResourceCmd::execute, FAILED_TO_DEPLOY_C8_RESOURCES + models);
    }
  }

}
