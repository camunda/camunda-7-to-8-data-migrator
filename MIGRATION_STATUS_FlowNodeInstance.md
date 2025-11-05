# FlowNodeInstance Model - Migration Status

## Model Information
- **C8 DbModel Class**: `FlowNodeInstanceDbModel`
- **Converter Class**: `FlowNodeConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| flowNodeInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricActivityInstance.getProcessDefinitionKey()` |
| flowNodeScopeKey | Long | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |
| startDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricActivityInstance.getStartTime()` |
| endDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricActivityInstance.getEndTime()` |
| flowNodeId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricActivityInstance.getActivityId()` |
| flowNodeName | String | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |
| treePath | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: Doesn't exist in C7, not yet supported by C8 RDBMS |
| type | FlowNodeType | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricActivityInstance.getActivityType()` via `convertType()` |
| state | FlowNodeState | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: Doesn't exist in C7, inherited from process instance |
| incidentKey | Long | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: Doesn't exist in C7 activity instance |
| numSubprocessIncidents | Long | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: increment/decrement when incident exists in subprocess, C8 RDBMS specific |
| hasIncident | Boolean | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricActivityInstance.getTenantId()` |
| partitionId | Integer | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |
| historyCleanupDate | OffsetDateTime | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |

## Summary Statistics
- **Total Fields**: 18
- **Migrated**: 8 (44.4%)
- **Set to Null (Nullify)**: 10 (55.6%)
- **Has TODO**: 4 (22.2%)
- **Needs Implementation**: 4 (22.2%)
- **Can Be Migrated**: 8 (44.4%)
- **Permanently Null (not in C7)**: 4 (22.2%)
- **Fully Migrated (non-null)**: 8 (44.4%)

## Outstanding Issues
1. **state**: Doesn't exist in C7 activity instance, inherited from process instance - TODO
2. **treePath**: Doesn't exist in C7 activity instance, not yet supported by C8 RDBMS - TODO
3. **incidentKey**: Doesn't exist in C7 activity instance - TODO
4. **numSubprocessIncidents**: C8 RDBMS specific, needs increment/decrement logic - TODO
5. **flowNodeScopeKey**: Missing from converter
6. **flowNodeName**: Missing from converter
7. **hasIncident**: Missing from converter
8. **partitionId**: Missing from converter (should be set to `C7_HISTORY_PARTITION_ID`)
9. **historyCleanupDate**: Missing from converter (should be migrated from C7)

## Type Conversion Logic
C7 ActivityType → C8 FlowNodeType (extensive mapping):
- `START_EVENT`, `START_EVENT_TIMER`, `START_EVENT_MESSAGE` → `START_EVENT`
- `END_EVENT_NONE`, `END_EVENT_CANCEL`, `END_EVENT_ERROR` → `END_EVENT`
- `TASK_SERVICE` → `SERVICE_TASK`
- `TASK_USER_TASK` → `USER_TASK`
- `GATEWAY_EXCLUSIVE` → `EXCLUSIVE_GATEWAY`
- `GATEWAY_PARALLEL` → `PARALLEL_GATEWAY`
- `INTERMEDIATE_EVENT_TIMER`, `INTERMEDIATE_EVENT_SIGNAL` → `INTERMEDIATE_CATCH_EVENT`
- `INTERMEDIATE_EVENT_COMPENSATION_THROW` → `INTERMEDIATE_THROW_EVENT`
- `TASK_BUSINESS_RULE` → `BUSINESS_RULE_TASK`
- `CALL_ACTIVITY` → `CALL_ACTIVITY`
- `TASK_SCRIPT` → `SCRIPT_TASK`
- `MULTI_INSTANCE_BODY` → `MULTI_INSTANCE_BODY`
- `SUB_PROCESS`, `TRANSACTION` → `SUB_PROCESS`
- `TASK_MANUAL_TASK` → `MANUAL_TASK`
- `TASK_RECEIVE_TASK` → `RECEIVE_TASK`
- `TASK` → `TASK`

## Notes
- Several fields are missing from the converter that should be populated
- The converter has extensive type mapping logic for various BPMN activity types
- There's a comment about TRANSACTION type: "TODO how to handle this?" but it maps to SUB_PROCESS

