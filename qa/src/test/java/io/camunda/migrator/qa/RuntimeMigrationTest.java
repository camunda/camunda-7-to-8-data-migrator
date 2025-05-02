package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ElementSelectors.byId;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.process.test.api.CamundaAssert;
import java.util.List;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RuntimeMigrationTest extends MigrationAbstractTest {

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  @Test
  public void simpleProcessMigrationTest() {
    // deploy processes
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/simpleProcess.bpmn");
    deployCamunda8Process("io/camunda/migrator/bpmn/c8/simpleProcess.bpmn");

    // given process state in c7
    var simpleProcess = runtimeService.startProcessInstanceByKey("simpleProcess");
    Task task1 = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task1.getId());
    Task task2 = taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult();
    ensureNotNull("Unexpected process state: userTask2 should exist", task2);
    ensureTrue("Unexpected process state: userTask2 should be 'created'", "created".equalsIgnoreCase(task2.getTaskState()));

    // when running runtime migration
    runtimeMigrator.migrate();

    // then there is one expected process instance
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(1, processInstances.size());
    ProcessInstance processInstance = processInstances.getFirst();
    assertEquals(simpleProcess.getProcessDefinitionKey(), processInstance.getProcessDefinitionId());

    // and the process instance has expected state
    ProcessInstanceEvent processInstanceEvent = new CustomProcessInstanceEvent(processInstance);
    CamundaAssert.assertThat(processInstanceEvent)
        .isActive()
        .hasActiveElements(byId("userTask2"))
        .hasVariable("legacyId", simpleProcess.getProcessInstanceId());
  }

}