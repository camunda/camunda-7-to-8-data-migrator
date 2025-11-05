# UserTask Model - Migration Status

## Model Information
- **C8 DbModel Class**: `UserTaskDbModel`
- **Converter Class**: `UserTaskConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| userTaskKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| elementId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getTaskDefinitionKey()` |
| name | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getName()` |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getProcessDefinitionKey()` |
| creationDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getStartTime()` |
| completionDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getEndTime()` |
| assignee | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getAssignee()` |
| state | UserTaskState | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getTaskState()` via `convertState()` |
| formKey | Long | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347 |
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessInstanceEntity.processInstanceKey()` |
| elementInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getTenantId()` via `getTenantId()` |
| dueDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getDueDate()` |
| followUpDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getFollowUpDate()` |
| candidateGroups | List<String> | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: ? |
| candidateUsers | List<String> | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: ? |
| externalFormReference | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: ? |
| processDefinitionVersion | Integer | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessInstanceEntity.processDefinitionVersion()` |
| customHeaders | Map<String,String> | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: ? |
| priority | Integer | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricTaskInstance.getPriority()` |
| partitionId | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Set to constant `C7_HISTORY_PARTITION_ID` |
| historyCleanupDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricTaskInstance.getRemovalTime()` |

## Summary Statistics
- **Total Fields**: 23
- **Migrated**: 18 (78.3%)
- **Set to Null (Nullify)**: 5 (21.7%)
- **Has TODO**: 5 (21.7%)
- **Needs Implementation**: 5 (21.7%)
- **Can Be Migrated**: 18 (78.3%)
- **Permanently Null (not in C7)**: 0 (0%)
- **Fully Migrated (non-null)**: 18 (78.3%)

## Outstanding Issues
1. **formKey**: Form key information - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347
2. **candidateGroups**: Candidate groups - TODO: ?
3. **candidateUsers**: Candidate users - TODO: ?
4. **externalFormReference**: External form reference - TODO: ?
5. **customHeaders**: Custom headers - TODO: ?

## State Conversion Logic
C7 TaskState → C8 UserTaskState:
- `Init`, `Created` → `CREATED`
- `Completed` → `COMPLETED`
- `Deleted` → `CANCELED`
- `Updated` → `CREATED`

## Notes
- UserTask state mapping converts various C7 states to simplified C8 states
- Several fields have TODO markers with "?" indicating uncertain implementation path

