/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.history;

import static io.camunda.migrator.MigratorMode.LIST_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.migrator.impl.clients.DbClient;
import io.camunda.migrator.impl.persistence.IdKeyMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:application-warn.properties")
@ExtendWith({OutputCaptureExtension.class})
public class HistoryMigrationListSkippedFilterTest extends HistoryMigrationAbstractTest {

    @Autowired
    protected DbClient dbClient;

    @Autowired
    private ManagementService managementService;

    @Autowired
    protected HistoryService historyService;

    @Test
    public void shouldListSkippedEntitiesWithSingleTypeFilter(CapturedOutput output) {
        // given multiple process instances with comprehensive entity generation
        deployer.deployCamunda7Process("comprehensiveSkippingTestProcess.bpmn");

        List<String> processInstanceIds = createTestProcessInstances();
        String processDefinitionId = getProcessDefinitionId();

        // Mark the process definition as skipped and run migration
        dbClient.insert(processDefinitionId, null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);
        historyMigrator.migrate();

        // when running history migration with list skipped mode and single entity type filter
        historyMigrator.setMode(LIST_SKIPPED);
        historyMigrator.setEntityTypesToPrint(List.of(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE));
        historyMigrator.start();

        // then verify the output contains only process instances
        Map<String, List<String>> skippedEntitiesByType = SkippedEntitiesLogParserUtils.parseSkippedEntitiesOutput(output.getOut());

        // Should only contain process instances
        assertThat(skippedEntitiesByType).hasSize(1);
        assertThat(skippedEntitiesByType).containsKey(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE.getDisplayName());
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE.getDisplayName()))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(processInstanceIds);

        // Should not contain other entity types
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION.getDisplayName());
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_USER_TASK.getDisplayName());
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_VARIABLE.getDisplayName());
    }

    @Test
    public void shouldListSkippedEntitiesWithMultipleTypeFilters(CapturedOutput output) {
        // given multiple process instances with comprehensive entity generation
        deployer.deployCamunda7Process("comprehensiveSkippingTestProcess.bpmn");

        List<String> processInstanceIds = createTestProcessInstances();
        String processDefinitionId = getProcessDefinitionId();

        // Mark the process definition as skipped and run migration
        dbClient.insert(processDefinitionId, null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);
        historyMigrator.migrate();

        // when running history migration with list skipped mode and multiple entity type filters
        historyMigrator.setMode(LIST_SKIPPED);
        historyMigrator.setEntityTypesToPrint(List.of(
            IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE,
            IdKeyMapper.TYPE.HISTORY_USER_TASK
        ));
        historyMigrator.start();

        // then verify the output contains only process instances and user tasks
        Map<String, List<String>> skippedEntitiesByType = SkippedEntitiesLogParserUtils.parseSkippedEntitiesOutput(output.getOut());

        // Should only contain process instances and user tasks
        assertThat(skippedEntitiesByType).hasSize(2);
        assertThat(skippedEntitiesByType).containsKeys(
            IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE.getDisplayName(),
            IdKeyMapper.TYPE.HISTORY_USER_TASK.getDisplayName()
        );

        // Verify process instances
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE.getDisplayName()))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(processInstanceIds);

        // Verify user tasks
        List<String> expectedUserTaskIds = historyService.createHistoricTaskInstanceQuery()
            .processDefinitionId(processDefinitionId)
            .list()
            .stream()
            .map(task -> task.getId())
            .collect(Collectors.toList());
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_USER_TASK.getDisplayName()))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(expectedUserTaskIds);

        // Should not contain other entity types
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION.getDisplayName());
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_VARIABLE.getDisplayName());
        assertThat(skippedEntitiesByType).doesNotContainKey(IdKeyMapper.TYPE.HISTORY_INCIDENT.getDisplayName());
    }

    // Common test utility methods - createTestProcessInstances and getProcessDefinitionId remain

    private List<String> createTestProcessInstances() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var processInstance = runtimeService.startProcessInstanceByKey("comprehensiveSkippingTestProcessId",
                Map.of("testVar", "testValue" + i, "anotherVar", "anotherValue" + i));
            processInstanceIds.add(processInstance.getId());

            // Complete user tasks to generate user task history
            var tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            for (Task task : tasks) {
                taskService.setVariableLocal(task.getId(), "taskLocalVar", "taskValue" + i);
                taskService.complete(task.getId());
            }

            // Execute failing service task jobs to generate incidents
            var jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
            for (var job : jobs) {
                for (int attempt = 0; attempt < 3; attempt++) {
                    try {
                        managementService.executeJob(job.getId());
                    } catch (Exception e) {
                        // Expected - job will fail and create incident
                    }
                }
            }
        }
        return processInstanceIds;
    }

    private String getProcessDefinitionId() {
        return repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("comprehensiveSkippingTestProcessId")
            .latestVersion()
            .singleResult()
            .getId();
    }
}
