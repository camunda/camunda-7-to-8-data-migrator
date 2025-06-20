package io.camunda.migrator.qa.variables;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.SpringProcessEnginePlugin;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestConfig extends SpringProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.getProcessEnginePlugins().add(new SpinProcessEnginePlugin());
  }

}
