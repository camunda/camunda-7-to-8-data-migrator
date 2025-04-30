package io.camunda.migrator.qa;

import static io.camunda.process.test.api.assertions.ElementSelectors.byName;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@CamundaSpringProcessTest
class RuntimeMigrationTest {

  @Autowired
  private RuntimeMigrator runtimeMigrator;

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private TaskService taskService;

  @Autowired
  private CamundaClient camundaClient;

  @Autowired
  private CamundaProcessTestContext processTestContext;

  @PostConstruct
  public void init() {
    runtimeMigrator.setCamundaClient(camundaClient);
    runtimeMigrator.setAutoDeployment(false);
  }

  @Test
  public void simpleProcessMigrationTest() {
    // deploy process for both c7 and c8
    repositoryService.createDeployment().addClasspathResource("resources/io/camunda/migrator/bpmn/c7/simpleProcess.bpmn").deploy();
    DeploymentEvent join = camundaClient.newDeployResourceCommand().addResourceFromClasspath("resources/io/camunda/migrator/bpmn/c8/simpleProcess.bpmn").send().join();

    // generate data in c7
    repositoryService.createDeployment().addClasspathResource("resources/io/camunda/migrator/bpmn/c7/simpleProcess.bpmn").deploy();
    var simpleProcess = runtimeService.startProcessInstanceByKey("simpleProcess");
    Task task1 = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.complete(task1.getId());
    Task task2 = taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult();
    ensureNotNull("Unexpected process state", task2);
    ensureTrue("Unexpected process state", "created".equalsIgnoreCase(task2.getTaskState()));

    // run migration
    runtimeMigrator.migrate();

    // assert on c8 data

    // process instance started
    List<ProcessInstance> processInstances = camundaClient.newProcessInstanceSearchRequest().send().join().items();
    assertEquals(1, processInstances.size());
    ProcessInstance processInstance = processInstances.getFirst();
    assertEquals(simpleProcess.getProcessDefinitionKey(), processInstance.getProcessDefinitionId());

    // process state
    ProcessInstanceEvent processInstanceEvent = new CustomProcessInstanceEvent(processInstance);
    CamundaAssert.assertThat(processInstanceEvent)
        .isActive()
        .hasActiveElements(byName("User Task 2")) // Failing because "User Task 2" is not activated yet
        .hasCompletedElements(byName("User Task 1")); // Failing because "User Task 1" is not completed but active
  }

}