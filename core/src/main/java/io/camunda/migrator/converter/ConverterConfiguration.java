package io.camunda.migrator.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConverterConfiguration {

  @Bean
  public DecisionDefinitionConverter decisionDefinitionConverter() {
    return new DecisionDefinitionConverter();
  }

  @Bean
  public FlowNodeConverter flowNodeConverter() {
    return new FlowNodeConverter();
  }

  @Bean
  public IncidentConverter incidentConverter() {
    return new IncidentConverter();
  }

  @Bean
  public ProcessDefinitionConverter processDefinitionConverter() {
    return new ProcessDefinitionConverter();
  }

  @Bean
  public ProcessInstanceConverter processInstanceConverter() {
    return new ProcessInstanceConverter();
  }

  @Bean
  public UserTaskConverter userTaskConverter() {
    return new UserTaskConverter();
  }

  @Bean
  public VariableConverter variableConverter() {
    return new VariableConverter();
  }

}
