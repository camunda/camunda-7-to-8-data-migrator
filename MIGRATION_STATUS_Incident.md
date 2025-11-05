# Incident Model - Migration Status

## Model Information
- **C8 DbModel Class**: `IncidentDbModel`
- **Converter Class**: `IncidentConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| incidentKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricIncident.getProcessDefinitionKey()` |
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| flowNodeInstanceKey | Long | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | Passed as parameter - TODO: verify if linking is correct |
| flowNodeId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricIncident.getActivityId()` |
| jobKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter (jobDefinitionKey) |
| errorType | ErrorType | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: does error type exist in C7? |
| errorMessage | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricIncident.getIncidentMessage()` |
| errorMessageHash | Integer | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - C8 specific field |
| creationDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricIncident.getCreateTime()` |
| state | IncidentState | ✅ | ⚠️ | ❌ | ✅ | ✅ | ❌ | ⚠️ | ❌ | Always set to hardcoded `0` - TODO: make `HistoricIncidentEventEntity#getIncidentState()` accessible |
| treePath | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: ? |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricIncident.getTenantId()` |
| partitionId | int | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |
| historyCleanupDate | OffsetDateTime | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by converter - missing from builder call |

## Summary Statistics
- **Total Fields**: 16
- **Migrated**: 10 (62.5%)
- **Set to Null (Nullify)**: 6 (37.5%)
- **Has TODO**: 5 (31.3%)
- **Needs Implementation**: 4 (25.0%)
- **Can Be Migrated**: 10 (62.5%)
- **Permanently Null (not in C7)**: 1 (6.3%)
- **Fully Migrated (non-null)**: 10 (62.5%)

## Outstanding Issues
1. **errorType**: Error type information - TODO: does error type exist in C7?
2. **state**: Currently hardcoded to `0` (ACTIVE) - TODO: make `HistoricIncidentEventEntity#getIncidentState()` accessible
3. **treePath**: Tree path information - TODO: ?
4. **flowNodeInstanceKey**: Has TODO comment to verify if linking is correct
5. **partitionId**: Missing from converter (should be set to `C7_HISTORY_PARTITION_ID`)
6. **historyCleanupDate**: Missing from converter (should be migrated from C7)
7. **errorMessageHash**: Not set - C8 specific field for indexing

## State Conversion Logic
Currently uses hardcoded state:
- `0` → `ACTIVE` (open)
- `1`, `2` → `RESOLVED` (resolved/deleted)

**Note**: The converter currently always passes `0`, so all incidents are marked as `ACTIVE`.

## Notes
- The converter is missing `partitionId` and `historyCleanupDate` in the builder call
- State detection needs implementation to access the actual incident state from C7

