package io.camunda.migrator.qa;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.migrator.RuntimeMigrator;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@CamundaSpringProcessTest
public abstract class RuntimeMigrationAbstractTest {

  @Autowired
  protected CamundaClient camundaClient;

  @Autowired
  protected RuntimeMigrator runtimeMigrator;

  @Autowired
  protected RepositoryService repositoryService;

  @PostConstruct
  public void init() {
    runtimeMigrator.setCamundaClient(camundaClient);
    runtimeMigrator.setAutoDeployment(false);
  }

  protected void deployCamunda7Process(String resourcePath) {
    Deployment deployment = repositoryService.createDeployment().addClasspathResource(resourcePath).deploy();
    if (deployment == null) {
      throw new IllegalStateException("Could not deploy process");
    }
  }

  protected void deployCamunda8Process(String resourcePath) {
    DeploymentEvent deployment = camundaClient.newDeployResourceCommand().addResourceFromClasspath(resourcePath).send().join();
    if (deployment == null) {
      throw new IllegalStateException("Could not deploy process");
    }
  }

  protected void deployProcessInC7AndC8(String fileName) {
    deployCamunda7Process("io/camunda/migrator/bpmn/c7/" + fileName);
    deployCamunda8Process("io/camunda/migrator/bpmn/c8/" + fileName);
  }
}
