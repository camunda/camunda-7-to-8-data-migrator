/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.converter;

import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.logging.ProcessDefinitionConverterLogs;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.camunda.migrator.impl.util.ConverterUtil.getNextKey;

public class ProcessDefinitionConverter {

  @Autowired
  private C7Client c7Client;

  public ProcessDefinitionDbModel apply(ProcessDefinition legacyProcessDefinition) {
    String bpmnXml = getBpmnXmlAsString(legacyProcessDefinition);

    return new ProcessDefinitionDbModel.ProcessDefinitionDbModelBuilder().processDefinitionKey(getNextKey())
        .processDefinitionId(legacyProcessDefinition.getKey())
        .resourceName(legacyProcessDefinition.getResourceName())
        .name(legacyProcessDefinition.getName())
        .tenantId(legacyProcessDefinition.getTenantId())
        .versionTag(legacyProcessDefinition.getVersionTag())
        .version(legacyProcessDefinition.getVersion())
        .bpmnXml(bpmnXml)
        .formId(null) // TODO https://github.com/camunda/camunda-bpm-platform/issues/5347
        .build();
  }

  private String getBpmnXmlAsString(ProcessDefinition processDefinition) {
    try {
      var resourceStream = c7Client.getResourceAsStream(processDefinition.getDeploymentId(),
          processDefinition.getResourceName());

      return readInputStreamToString(resourceStream);
    } catch (IOException e) {
      ProcessDefinitionConverterLogs.failedFetchingResourceStream(processDefinition.getId(), e.getMessage());
      return null;
    }
  }

  private String readInputStreamToString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];

    for (int length; (length = inputStream.read(buffer)) != -1; ) {
      result.write(buffer, 0, length);
    }

    return result.toString(StandardCharsets.UTF_8);
  }
}
