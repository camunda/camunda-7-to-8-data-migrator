/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.qa.history.HistoryMigrationAbstractTest;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.entities.UserTaskEntity;
import java.util.Date;
import java.util.List;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoryUserTaskTest extends HistoryMigrationAbstractTest {

  @Autowired
  protected HistoryService historyService;

  @Test
  public void shouldMigrateUserTaskBasicFields() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setAssignee(task.getId(), "testUser");
    taskService.setPriority(task.getId(), 75);
    taskService.complete(task.getId());

    HistoricTaskInstance c7Task = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(1);

    UserTaskEntity userTask = userTasks.getFirst();
    assertUserTaskFields(userTask, c7Task, migratedProcessInstance, "userTaskId");
  }

  @Test
  public void shouldMigrateUserTaskWithTenant() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn", "my-tenant1");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setAssignee(task.getId(), "tenantUser");
    taskService.complete(task.getId());

    HistoricTaskInstance c7Task = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(1);

    UserTaskEntity userTask = userTasks.getFirst();
    assertThat(userTask.tenantId()).isEqualTo("my-tenant1");
    assertUserTaskFields(userTask, c7Task, migratedProcessInstance, "userTaskId");
  }

  @Test
  public void shouldMigrateUserTaskWithDatesAndPriority() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set task properties
    Date dueDate = new Date();
    Date followUpDate = new Date(dueDate.getTime() + 86400000); // 1 day after due date
    taskService.setDueDate(task.getId(), dueDate);
    taskService.setFollowUpDate(task.getId(), followUpDate);
    taskService.setPriority(task.getId(), 50);
    taskService.setAssignee(task.getId(), "assignedUser");

    taskService.complete(task.getId());

    HistoricTaskInstance c7Task = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(1);

    UserTaskEntity userTask = userTasks.getFirst();
    assertThat(userTask.priority()).isEqualTo(50);
    assertThat(userTask.assignee()).isEqualTo("assignedUser");
    assertThat(userTask.dueDate()).isNotNull();
    assertThat(userTask.followUpDate()).isNotNull();
    assertUserTaskFields(userTask, c7Task, migratedProcessInstance, "userTaskId");
  }

  @Test
  public void shouldMigrateUserTaskStates() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn");

    // Test completed task
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("userTaskProcessId");
    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
    taskService.complete(task1.getId());

    // Test active (created) task
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(2);

    // Check completed task
    ProcessInstanceEntity completedProcessInstance = processInstances.stream()
        .filter(pi -> pi.state() == ProcessInstanceEntity.ProcessInstanceState.COMPLETED)
        .findFirst().orElseThrow();

    List<UserTaskEntity> completedUserTasks = searchHistoricUserTasks(completedProcessInstance.processInstanceKey());
    assertThat(completedUserTasks).hasSize(1);
    assertThat(completedUserTasks.getFirst().state()).isEqualTo(UserTaskEntity.UserTaskState.COMPLETED);
    assertThat(completedUserTasks.getFirst().completionDate()).isNotNull();

    // Check active task
    ProcessInstanceEntity activeProcessInstance = processInstances.stream()
        .filter(pi -> pi.state() == ProcessInstanceEntity.ProcessInstanceState.ACTIVE)
        .findFirst().orElseThrow();

    List<UserTaskEntity> activeUserTasks = searchHistoricUserTasks(activeProcessInstance.processInstanceKey());
    assertThat(activeUserTasks).hasSize(1);
    assertThat(activeUserTasks.getFirst().state()).isEqualTo(UserTaskEntity.UserTaskState.CREATED);
    assertThat(activeUserTasks.getFirst().completionDate()).isNull();
  }

  @Test
  public void shouldMigrateMultipleUserTasks() {
    // given
    deployer.deployCamunda7Process("simpleProcess.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleProcess");

    // Complete first user task
    Task task1 = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.setAssignee(task1.getId(), "user1");
    taskService.complete(task1.getId());

    // Leave second user task active
    Task task2 = taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult();
    taskService.setAssignee(task2.getId(), "user2");

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("simpleProcess");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(2);

    // Verify both tasks were migrated with correct states and assignees
    UserTaskEntity completedTask = userTasks.stream()
        .filter(ut -> ut.state() == UserTaskEntity.UserTaskState.COMPLETED)
        .findFirst().orElseThrow();
    assertThat(completedTask.assignee()).isEqualTo("user1");
    assertThat(completedTask.elementId()).isEqualTo("userTask1");

    UserTaskEntity activeTask = userTasks.stream()
        .filter(ut -> ut.state() == UserTaskEntity.UserTaskState.CREATED)
        .findFirst().orElseThrow();
    assertThat(activeTask.assignee()).isEqualTo("user2");
    assertThat(activeTask.elementId()).isEqualTo("userTask2");
  }

  @Test
  public void shouldMigrateUserTaskWithNoAssignee() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    // Don't set assignee - leave it null
    taskService.complete(task.getId());

    HistoricTaskInstance c7Task = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(1);

    UserTaskEntity userTask = userTasks.getFirst();
    assertThat(userTask.assignee()).isNull();
    assertUserTaskFields(userTask, c7Task, migratedProcessInstance);
  }

  @Test
  public void shouldMigrateUserTaskWithNullDates() {
    // given
    deployer.deployCamunda7Process("userTaskProcess.bpmn");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskProcessId");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    // Leave due date and follow up date as null
    taskService.complete(task.getId());

    HistoricTaskInstance c7Task = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    historyMigrator.migrate();

    // then
    List<ProcessInstanceEntity> processInstances = searchHistoricProcessInstances("userTaskProcessId");
    assertThat(processInstances).hasSize(1);

    ProcessInstanceEntity migratedProcessInstance = processInstances.getFirst();
    List<UserTaskEntity> userTasks = searchHistoricUserTasks(migratedProcessInstance.processInstanceKey());
    assertThat(userTasks).hasSize(1);

    UserTaskEntity userTask = userTasks.getFirst();
    assertThat(userTask.dueDate()).isNull();
    assertThat(userTask.followUpDate()).isNull();
    assertUserTaskFields(userTask, c7Task, migratedProcessInstance);
  }

  private void assertUserTaskFields(UserTaskEntity userTask,
                                    HistoricTaskInstance c7Task,
                                    ProcessInstanceEntity processInstance) {
    // Basic identifiers
    assertThat(userTask.userTaskKey()).isNotNull();
    assertThat(userTask.elementId()).isNotNull();
    assertThat(userTask.processDefinitionId()).isEqualTo(c7Task.getProcessDefinitionKey());
    assertThat(userTask.processDefinitionKey()).isNotNull();
    assertThat(userTask.processInstanceKey()).isEqualTo(processInstance.processInstanceKey());
    assertThat(userTask.elementInstanceKey()).isNotNull();

    // Dates
    assertThat(userTask.creationDate()).isNotNull();
    if (c7Task.getEndTime() != null) {
      assertThat(userTask.completionDate()).isNotNull();
    }

    // Task properties
    assertThat(userTask.assignee()).isEqualTo(c7Task.getAssignee());
    assertThat(userTask.priority()).isEqualTo(c7Task.getPriority());
    assertThat(userTask.name()).isEqualTo(c7Task.getName());

    // Tenant
    assertThat(userTask.tenantId()).isEqualTo(c7Task.getTenantId());

    // Process definition version
    assertThat(userTask.processDefinitionVersion()).isEqualTo(processInstance.processDefinitionVersion());

    // Fields that are currently null in converter (TODOs)
    assertThat(userTask.formKey()).isNull();
    assertThat(userTask.candidateGroups()).isNull();
    assertThat(userTask.candidateUsers()).isNull();
    assertThat(userTask.externalFormReference()).isNull();
    assertThat(userTask.customHeaders()).isNull();
  }

  private void assertUserTaskFields(UserTaskEntity userTask,
                                    HistoricTaskInstance c7Task,
                                    ProcessInstanceEntity processInstance,
                                    String expectedElementId) {
    // Basic identifiers
    assertThat(userTask.userTaskKey()).isNotNull();
    assertThat(userTask.elementId()).isEqualTo(expectedElementId);
    assertThat(userTask.processDefinitionId()).isEqualTo(c7Task.getProcessDefinitionKey());
    assertThat(userTask.processDefinitionKey()).isNotNull();
    assertThat(userTask.processInstanceKey()).isEqualTo(processInstance.processInstanceKey());
    assertThat(userTask.elementInstanceKey()).isNotNull();

    // Dates
    assertThat(userTask.creationDate()).isNotNull();
    if (c7Task.getEndTime() != null) {
      assertThat(userTask.completionDate()).isNotNull();
    }

    // Task properties
    assertThat(userTask.assignee()).isEqualTo(c7Task.getAssignee());
    assertThat(userTask.priority()).isEqualTo(c7Task.getPriority());
    assertThat(userTask.name()).isEqualTo(c7Task.getName());

    // Tenant
    assertThat(userTask.tenantId()).isEqualTo(c7Task.getTenantId());

    // Process definition version
    assertThat(userTask.processDefinitionVersion()).isEqualTo(processInstance.processDefinitionVersion());

    // Fields that are currently null in converter (TODOs)
    assertThat(userTask.formKey()).isNull();
    assertThat(userTask.candidateGroups()).isNull();
    assertThat(userTask.candidateUsers()).isNull();
    assertThat(userTask.externalFormReference()).isNull();
    assertThat(userTask.customHeaders()).isNull();
  }
}
