/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.code.recipes.client.migrate;

import static org.openrewrite.java.Assertions.java;

import io.camunda.migration.code.recipes.client.MigrateProcessInstanceQueryMethodsRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class ReplaceProcessInstanceQueryMethodsTest implements RewriteTest {

  @Test
  void replaceProcessInstanceQueryMethods() {
    rewriteRun(spec -> spec.recipe(new MigrateProcessInstanceQueryMethodsRecipe()),
        java(
"""
package org.camunda.community.migration.example;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import io.camunda.client.CamundaClient;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.List;

@Component
public class HandleProcessInstanceQueryMethodsTestClass {

    @Autowired
    private ProcessEngine engine;

    @Autowired
    private CamundaClient camundaClient;

    public void processInstanceQueryMethods(String activityIdIn, String businessKey, String processDefinitionKey) {

        engine.getRuntimeService().createProcessInstanceQuery()
                .activityIdIn(activityIdIn)
                .active()
                .list();

        engine.getRuntimeService().createProcessInstanceQuery()
               .processInstanceBusinessKey(businessKey)
               .active()
               .list();

        engine.getRuntimeService().createProcessInstanceQuery()
               .processInstanceBusinessKey(businessKey)
               .processDefinitionKey(processDefinitionKey);

        engine.getRuntimeService().createProcessInstanceQuery()
               .processDefinitionKey(processDefinitionKey)
               .list();
    }
}
""",
"""
package org.camunda.community.migration.example;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import io.camunda.client.CamundaClient;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.List;

@Component
public class HandleProcessInstanceQueryMethodsTestClass {

    @Autowired
    private ProcessEngine engine;

    @Autowired
    private CamundaClient camundaClient;

    public void processInstanceQueryMethods(String activityIdIn, String businessKey, String processDefinitionKey) {

        camundaClient
                .newProcessInstanceSearchRequest()
                .filter(filter -> filter
                        .elementId(activityIdIn)
                        .state(ProcessInstanceState.ACTIVE))
                .send()
                .join()
                .items();

        // TODO: processInstanceBusinessKey was removed - use businessId (Camunda 8.9+) instead
        camundaClient
                .newProcessInstanceSearchRequest()
                .filter(filter -> filter
                        .state(ProcessInstanceState.ACTIVE))
                .send()
                .join()
                .items();

        // TODO: processInstanceBusinessKey was removed - use businessId (Camunda 8.9+) instead
        camundaClient
                .newProcessInstanceSearchRequest()
                .filter(filter -> filter.processDefinitionId(processDefinitionKey));

        camundaClient
                .newProcessInstanceSearchRequest()
                .filter(filter -> filter
                        .processDefinitionId(processDefinitionKey)
                        .state(ProcessInstanceState.ACTIVE))
                .send()
                .join()
                .items();
    }
}
"""
        ));
  }

  @Test
  void doesNotRewriteTaskQueryListChains() {
    rewriteRun(
        spec -> spec.recipe(new MigrateProcessInstanceQueryMethodsRecipe()),
        java(
            """
            package org.camunda.community.migration.example;

            import org.camunda.bpm.engine.ProcessEngine;
            import org.camunda.bpm.engine.task.Task;
            import io.camunda.client.CamundaClient;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            import java.util.List;

            @Component
            public class MixedQueryTypesTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void mixedQueries(String processDefinitionKey) {
                    engine.getRuntimeService().createProcessInstanceQuery()
                            .processDefinitionKey(processDefinitionKey)
                            .list();

                    int taskCount = engine.getTaskService().createTaskQuery()
                            .list()
                            .size();

                    List<Task> tasks = engine.getTaskService().createTaskQuery()
                            .list();

                    engine.getTaskService().createTaskQuery()
                            .processDefinitionKey(processDefinitionKey)
                            .list();
                }
            }
            """,
            """
            package org.camunda.community.migration.example;
            import io.camunda.client.api.search.enums.ProcessInstanceState;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.camunda.bpm.engine.task.Task;
            import io.camunda.client.CamundaClient;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            import java.util.List;

            @Component
            public class MixedQueryTypesTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void mixedQueries(String processDefinitionKey) {
                    camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter
                                    .processDefinitionId(processDefinitionKey)
                                    .state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .items();

                    int taskCount = engine.getTaskService().createTaskQuery()
                            .list()
                            .size();

                    List<Task> tasks = engine.getTaskService().createTaskQuery()
                            .list();

                    engine.getTaskService().createTaskQuery()
                            .processDefinitionKey(processDefinitionKey)
                            .list();
                }
            }
            """));
  }

  @Test
  void warnsWhenBusinessKeyIsCombinedWithProcessDefinitionKey() {
    rewriteRun(
        spec -> spec.recipe(new MigrateProcessInstanceQueryMethodsRecipe()),
        java(
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            import java.util.List;

            @Component
            public class CombinedQueryTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void combinedQuery(String businessKey, String processDefinitionKey) {
                    engine.getRuntimeService().createProcessInstanceQuery()
                            .processInstanceBusinessKey(businessKey)
                            .processDefinitionKey(processDefinitionKey)
                            .list();
                }
            }
            """,
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import io.camunda.client.api.search.enums.ProcessInstanceState;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            import java.util.List;

            @Component
            public class CombinedQueryTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void combinedQuery(String businessKey, String processDefinitionKey) {
                    // TODO: processInstanceBusinessKey was removed - use businessId (Camunda 8.9+) instead
                    camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter
                                    .processDefinitionId(processDefinitionKey)
                                    .state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .items();
                }
            }
            """));
  }

  @Test
  void replacesUnfilteredProcessInstanceCounts() {
    rewriteRun(
        spec -> spec.recipe(new MigrateProcessInstanceQueryMethodsRecipe()),
        java(
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class UnfilteredProcessInstanceCountsTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void countQueries() {
                    int listSize = engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .active()
                            .list()
                            .size();

                    long streamCount = engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .active()
                            .list()
                            .stream()
                            .count();

                    long directCount = engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .active()
                            .count();

                }
            }
            """,
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import io.camunda.client.api.search.enums.ProcessInstanceState;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class UnfilteredProcessInstanceCountsTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void countQueries() {
                    Long listSize = camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter.state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .page()
                            .totalItems();

                    Long streamCount = camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter.state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .page()
                            .totalItems();

                    Long directCount = camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter.state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .page()
                            .totalItems();

                }
            }
            """));
  }

  @Test
  void replacesFilteredProcessInstanceCounts() {
    rewriteRun(
        spec -> spec.recipe(new MigrateProcessInstanceQueryMethodsRecipe()),
        java(
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class FilteredProcessInstanceCountsTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void countQueries(String activityId, String businessKey) {
                    long activityCount = engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .activityIdIn(activityId)
                            .count();

                    long businessCount = engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceBusinessKey(businessKey)
                            .count();
                }
            }
            """,
            """
            package org.camunda.community.migration.example;

            import io.camunda.client.CamundaClient;
            import io.camunda.client.api.search.enums.ProcessInstanceState;
            import org.camunda.bpm.engine.ProcessEngine;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class FilteredProcessInstanceCountsTestClass {

                @Autowired
                private ProcessEngine engine;

                @Autowired
                private CamundaClient camundaClient;

                public void countQueries(String activityId, String businessKey) {
                    Long activityCount = camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter
                                    .elementId(activityId)
                                    .state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .page()
                            .totalItems();

                    // TODO: processInstanceBusinessKey was removed - use businessId (Camunda 8.9+) instead
                    Long businessCount = camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(filter -> filter.state(ProcessInstanceState.ACTIVE))
                            .send()
                            .join()
                            .page()
                            .totalItems();
                }
            }
            """));
  }
}
