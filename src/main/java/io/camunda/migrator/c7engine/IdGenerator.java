package io.camunda.migrator.c7engine;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.SpringProcessEnginePlugin;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class IdGenerator extends SpringProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.setIdGenerator(() -> new Random().nextLong() + "");
  }
}
