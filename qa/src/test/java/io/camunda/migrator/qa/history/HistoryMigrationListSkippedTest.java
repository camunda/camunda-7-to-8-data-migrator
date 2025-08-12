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

import io.camunda.migrator.HistoryMigrator;
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
public class HistoryMigrationListSkippedTest extends HistoryMigrationAbstractTest {

    @Autowired
    protected DbClient dbClient;

    @Autowired
    private ManagementService managementService;

    @Autowired
    protected HistoryService historyService;

    @Test
    public void shouldListAllSkippedHistoricEntities(CapturedOutput output) {
        // given multiple process instances with comprehensive entity generation
        deployer.deployCamunda7Process("comprehensiveSkippingTestProcess.bpmn");

        List<String> processInstanceIds = createTestProcessInstances();
        String processDefinitionId = getProcessDefinitionId();

        // Verify expected entities exist in C7
        verifyC7EntitiesExist();

        // Mark the process definition as skipped and run migration
        dbClient.insert(processDefinitionId, null, IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION);
        historyMigrator.migrate();

        // Verify all entities were marked as skipped
        verifyEntitiesMarkedAsSkipped();

        // when running history migration with list skipped mode
        historyMigrator.setMode(LIST_SKIPPED);
        historyMigrator.start();

        // then verify the output contains all expected skipped entities
        Map<String, List<String>> skippedEntitiesByType = parseSkippedEntitiesOutput(output.getOut());
        verifySkippedEntitiesOutput(skippedEntitiesByType, processDefinitionId, processInstanceIds);
    }

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

    private void verifyC7EntitiesExist() {
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(3);
        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(3);
        assertThat(historyService.createHistoricVariableInstanceQuery().count()).isGreaterThan(6);
        assertThat(historyService.createHistoricIncidentQuery().count()).isEqualTo(3);
        assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(12);
    }

    private void verifyEntitiesMarkedAsSkipped() {
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION)).isEqualTo(1);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE)).isEqualTo(3);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_USER_TASK)).isEqualTo(3);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_INCIDENT)).isEqualTo(3);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_VARIABLE)).isGreaterThan(6);
        assertThat(dbClient.countSkippedByType(IdKeyMapper.TYPE.HISTORY_FLOW_NODE)).isEqualTo(12);
    }

    private void verifySkippedEntitiesOutput(Map<String, List<String>> skippedEntitiesByType,
                                           String processDefinitionId, List<String> processInstanceIds) {
        // Verify all expected entity types are present
        String[] expectedEntityTypes = HistoryMigrator.HISTORY_TYPES.stream()
            .map(IdKeyMapper.TYPE::getDisplayName)
            .toArray(String[]::new);

        assertThat(skippedEntitiesByType).containsKeys(expectedEntityTypes);

        // Verify specific entities with expected counts and IDs
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_PROCESS_DEFINITION.getDisplayName()))
            .hasSize(1)
            .containsExactly(processDefinitionId);

        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_PROCESS_INSTANCE.getDisplayName()))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(processInstanceIds);

        verifyHistoricEntitiesById(skippedEntitiesByType, processDefinitionId);

        // Verify empty entity types
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_DECISION_DEFINITION.getDisplayName())).isEmpty();
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_DECISION_INSTANCE.getDisplayName())).isEmpty();
    }

    private void verifyHistoricEntitiesById(Map<String, List<String>> skippedEntitiesByType, String processDefinitionId) {
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

        // Verify incidents
        List<String> expectedIncidentIds = historyService.createHistoricIncidentQuery()
            .processDefinitionId(processDefinitionId)
            .list()
            .stream()
            .map(incident -> incident.getId())
            .collect(Collectors.toList());
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_INCIDENT.getDisplayName()))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(expectedIncidentIds);

        // Verify variables
        List<String> expectedVariableIds = historyService.createHistoricVariableInstanceQuery()
            .processDefinitionId(processDefinitionId)
            .list()
            .stream()
            .map(variable -> variable.getId())
            .collect(Collectors.toList());
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_VARIABLE.getDisplayName()))
            .hasSize(9)
            .containsAll(expectedVariableIds);

        // Verify flow nodes
        List<String> expectedFlowNodeIds = historyService.createHistoricActivityInstanceQuery()
            .processDefinitionId(processDefinitionId)
            .list()
            .stream()
            .map(activity -> activity.getId())
            .collect(Collectors.toList());
        assertThat(skippedEntitiesByType.get(IdKeyMapper.TYPE.HISTORY_FLOW_NODE.getDisplayName()))
            .hasSize(12)
            .containsExactlyInAnyOrderElementsOf(expectedFlowNodeIds);
    }

    /**
     * Parses the migrator output and extracts skipped entities.
     * Filters out debug/info logs that may appear in CI environments.
     */
    private Map<String, List<String>> parseSkippedEntitiesOutput(String output) {
        Map<String, List<String>> result = new HashMap<>();

        String relevantOutput = extractRelevantOutput(output);
        if (relevantOutput.isEmpty()) {
            return result;
        }

        List<EntitySection> sections = extractEntitySections(relevantOutput);

        for (EntitySection section : sections) {
            List<String> entityIds = extractEntityIds(section.content);
            result.put(section.entityType, entityIds);
        }

        return result;
    }

    private String extractRelevantOutput(String output) {
        Pattern startPattern = Pattern.compile("(Previously skipped \\[[^\\]]+\\]:|No entities of type \\[[^\\]]+\\] were skipped during previous migration)");
        Matcher matcher = startPattern.matcher(output);
        return matcher.find() ? output.substring(matcher.start()) : "";
    }

    private List<EntitySection> extractEntitySections(String output) {
        List<EntitySection> sections = new ArrayList<>();
        Pattern headerPattern = Pattern.compile("(Previously skipped \\[([^\\]]+)\\]:|No entities of type \\[([^\\]]+)\\] were skipped during previous migration)");
        Matcher matcher = headerPattern.matcher(output);

        int lastEnd = 0;
        String lastEntityType = null;

        while (matcher.find()) {
            // Process previous section if exists
            if (lastEntityType != null) {
                String content = output.substring(lastEnd, matcher.start()).trim();
                sections.add(new EntitySection(lastEntityType, content));
            }

            // Extract entity type from current match and remove trailing 's' to match enum display names
            String entityType = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            // For "Previously skipped" format, remove trailing 's'; for "No entities" format, keep as is
            lastEntityType = matcher.group(2) != null && entityType.endsWith("s") ?
                entityType.substring(0, entityType.length() - 1) : entityType;
            lastEnd = matcher.end();
        }

        // Process last section
        if (lastEntityType != null) {
            String content = output.substring(lastEnd).trim();
            sections.add(new EntitySection(lastEntityType, content));
        }

        return sections;
    }

    private List<String> extractEntityIds(String sectionContent) {
        if (sectionContent.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(sectionContent.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !isLogLine(line))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a line is a log message that should be filtered out in CI environments.
     * This is necessary because CI may output debug/info logs mixed with actual entity IDs.
     */
    private boolean isLogLine(String line) {
        return line.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z.*") || // ISO timestamp
               line.matches(".*(DEBUG|INFO|WARN|ERROR).*") || // Log levels
               line.matches(".*(==>|<==|Preparing:|Parameters:|Total:).*"); // SQL logging patterns
    }

    private static class EntitySection {
        final String entityType;
        final String content;

        EntitySection(String entityType, String content) {
            this.entityType = entityType;
            this.content = content;
        }
    }
}
