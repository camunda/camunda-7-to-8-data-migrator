# DecisionInstance Model - Migration Status

## Model Information
- **C8 DbModel Class**: `DecisionInstanceDbModel`
- **Converter Class**: `DecisionInstanceConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| decisionInstanceId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Migrated from composite: `{decisionInstanceKey}-{C7.id}` |
| decisionInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| state | DecisionInstanceState | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370 |
| evaluationDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricDecisionInstance.getEvaluationTime()` |
| evaluationFailure | String | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Set to `null` - not stored in HistoricDecisionInstance |
| evaluationFailureMessage | String | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Set to `null` - not stored in HistoricDecisionInstance |
| result | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricDecisionInstance.getCollectResultValue()` |
| flowNodeInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| flowNodeId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricDecisionInstance.getProcessDefinitionKey()` |
| decisionDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| decisionDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricDecisionInstance.getDecisionDefinitionKey()` |
| decisionRequirementsKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| decisionRequirementsId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricDecisionInstance.getDecisionRequirementsDefinitionKey()` |
| rootDecisionDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| decisionType | DecisionDefinitionType | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370 |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricDecisionInstance.getTenantId()` via `getTenantId()` |
| partitionId | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Set to constant `C7_HISTORY_PARTITION_ID` |
| evaluatedInputs | List<EvaluatedInput> | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Mapped from `HistoricDecisionInstance.getInputs()` |
| evaluatedOutputs | List<EvaluatedOutput> | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Mapped from `HistoricDecisionInstance.getOutputs()` |
| historyCleanupDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricDecisionInstance.getRemovalTime()` |

## Nested Objects

### EvaluatedInput
| Field Name | Type | Is Migrated | Source |
|------------|------|-------------|--------|
| decisionInstanceId | String | ✅ | From parent DecisionInstance.id |
| id | String | ✅ | From `HistoricDecisionInputInstance.getId()` |
| name | String | ✅ | From `HistoricDecisionInputInstance.getClauseName()` |
| value | String | ✅ | From `HistoricDecisionInputInstance.getValue()` (converted to String) |

### EvaluatedOutput
| Field Name | Type | Is Migrated | Source |
|------------|------|-------------|--------|
| decisionInstanceId | String | ✅ | From parent DecisionInstance.id |
| id | String | ✅ | From `HistoricDecisionOutputInstance.getId()` |
| name | String | ✅ | From `HistoricDecisionOutputInstance.getClauseName()` |
| value | String | ✅ | From `HistoricDecisionOutputInstance.getValue()` (converted to String) |
| ruleId | String | ✅ | From `HistoricDecisionOutputInstance.getRuleId()` |
| ruleIndex | Integer | ✅ | From `HistoricDecisionOutputInstance.getRuleOrder()` |

## Summary Statistics
- **Total Fields**: 23
- **Migrated**: 19 (82.6%)
- **Set to Null (Nullify)**: 4 (17.4%)
- **Has TODO**: 2 (8.7%)
- **Needs Implementation**: 2 (8.7%)
- **Can Be Migrated**: 19 (82.6%)
- **Permanently Null (not in C7)**: 2 (8.7%)
- **Fully Migrated (non-null)**: 19 (82.6%)

## Outstanding Issues
1. **state** (DecisionInstanceState): Not available in C7 - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370
2. **evaluationFailure**: Not stored in C7 HistoricDecisionInstance
3. **evaluationFailureMessage**: Not stored in C7 HistoricDecisionInstance
4. **decisionType** (DecisionDefinitionType): Not available in C7 - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370

