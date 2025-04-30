package io.camunda.migrator.qa;

import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessInstance;

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
