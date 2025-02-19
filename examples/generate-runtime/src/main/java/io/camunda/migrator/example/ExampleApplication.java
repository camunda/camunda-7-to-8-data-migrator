package io.camunda.migrator.example;

import jakarta.annotation.PostConstruct;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication {

  @Autowired
  protected RuntimeService runtimeService;

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  @PostConstruct
  public void generateRuntimeData() {
    runtimeService.startProcessInstanceByKey("cornercasesProcess", Variables.putValue("myGlobalVar", 1234)).getId();
  }

}
