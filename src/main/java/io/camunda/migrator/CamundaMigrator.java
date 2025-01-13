package io.camunda.migrator;

import io.camunda.db.rdbms.sql.ProcessInstanceMapper;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CamundaMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMigrator.class);

  @Autowired
  public ProcessInstanceMapper processInstanceMapper;

  @Bean
  public String migrate() {
    LOGGER.info("Migrating process instances");
    processInstanceMapper.insert(new ProcessInstanceDbModel(
        1L,
        "123",
        1L,
        null, // processInstanceState
        null, // offsetDateTime startDate
        null, // offsetDateTime endDate
        "tenantId",
        1L, // parentProcessInstanceKey
        1L, // parentElementInstanceKey
        1,  //numIncidents
        "elementId",
        1 // version
    ));

    return "";
  }

}
