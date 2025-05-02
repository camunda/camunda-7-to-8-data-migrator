package io.camunda.migrator.qa;

import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessInstance;

/**
 * The {@link io.camunda.process.test.api.CamundaAssert} library takes a {@link io.camunda.client.api.response.ProcessInstanceEvent} object.
 * This class is only returned when creating the process instance, which is done in the migrator,
 * so we need to wrap the {@link io.camunda.client.api.search.response.ProcessInstance} to do assertions (only the processInstanceKey is used).
 */
class CustomProcessInstanceEvent implements ProcessInstanceEvent {

  protected ProcessInstance processInstance;

  CustomProcessInstanceEvent(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }

  @Override
  public long getProcessDefinitionKey() {
    return processInstance.getProcessDefinitionKey();
  }

  @Override
  public String getBpmnProcessId() {
    return processInstance.getProcessDefinitionId();
  }

  @Override
  public int getVersion() {
    return processInstance.getProcessDefinitionVersion();
  }

  @Override
  public long getProcessInstanceKey() {
    return processInstance.getProcessInstanceKey();
  }

  @Override
  public String getTenantId() {
    return processInstance.getTenantId();
  }
}
