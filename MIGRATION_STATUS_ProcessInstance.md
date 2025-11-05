# ProcessInstance Model - Migration Status

## Model Information
- **C8 DbModel Class**: `ProcessInstanceDbModel`
- **Converter Class**: `ProcessInstanceConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricProcessInstance.getProcessDefinitionKey()` |
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| state | ProcessInstanceState | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricProcessInstance.getState()` via `convertState()` |
| startDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricProcessInstance.getStartTime()` |
| endDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricProcessInstance.getEndTime()` |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricProcessInstance.getTenantId()` via `getTenantId()` |
| parentProcessInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| parentElementInstanceKey | Long | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Commented out - TODO: Call activity instance id - https://github.com/camunda/camunda-bpm-platform/issues/5359 |
| numIncidents | Integer | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Commented out - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5400 |
| version | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricProcessInstance.getProcessDefinitionVersion()` |
| partitionId | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Set to constant `C7_HISTORY_PARTITION_ID` |
| treePath | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Commented out - TODO: Call activity instance id - https://github.com/camunda/camunda-bpm-platform/issues/5359 |
| historyCleanupDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricProcessInstance.getRemovalTime()` |

## Summary Statistics
- **Total Fields**: 14
- **Migrated**: 11 (78.6%)
- **Set to Null (Nullify)**: 3 (21.4%)
- **Has TODO**: 3 (21.4%)
- **Needs Implementation**: 3 (21.4%)
- **Can Be Migrated**: 11 (78.6%)
- **Permanently Null (not in C7)**: 0 (0%)
- **Fully Migrated (non-null)**: 11 (78.6%)

## Outstanding Issues
1. **parentElementInstanceKey**: Call activity instance id that created the process in C8. Not yet migrated from C7 - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5359
2. **numIncidents**: Number of incidents - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5400
3. **treePath**: Tree path information - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5359

## State Conversion Logic
C7 State → C8 State:
- `ACTIVE`, `SUSPENDED` → `ACTIVE`
- `COMPLETED` → `COMPLETED`
- `EXTERNALLY_TERMINATED`, `INTERNALLY_TERMINATED` → `CANCELED`

