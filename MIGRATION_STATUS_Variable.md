# Variable Model - Migration Status

## Model Information
- **C8 DbModel Class**: `VariableDbModel`
- **Converter Class**: `VariableConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| variableKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| name | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricVariableInstance.getName()` |
| type | ValueTypeEnum | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by builder - all variables mapped to String type currently |
| doubleValue | Double | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by builder - all variables mapped to String type currently |
| longValue | Long | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by builder - all variables mapped to String type currently |
| value | String | ✅ | ✅ | ❓ | ✅ | ❌ | ❌ | ✅ | ❌ | Converted via `convertValue()` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5329 |
| fullValue | String | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by builder - only set when value is truncated |
| isPreview | boolean | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | Not set by builder - only set when value is truncated |
| scopeKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processInstanceKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Passed as parameter from parent context |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricVariableInstance.getProcessDefinitionKey()` |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `HistoricVariableInstance.getTenantId()` via `getTenantId()` |
| partitionId | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Set to constant `C7_HISTORY_PARTITION_ID` |
| historyCleanupDate | OffsetDateTime | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Converted from `HistoricVariableInstance.getRemovalTime()` |

## Summary Statistics
- **Total Fields**: 14
- **Migrated**: 8 (57.1%)
- **Set to Null (Nullify)**: 6 (42.9%)
- **Has TODO**: 1 (7.1%)
- **Needs Implementation**: 0 (0%)
- **Can Be Migrated**: 8 (57.1%)
- **Permanently Null (not in C7)**: 5 (35.7%)
- **Fully Migrated (non-null)**: 8 (57.1%)

## Outstanding Issues
1. **value**: Variable value conversion - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5329
2. **type, doubleValue, longValue**: Currently the VariableDbModelBuilder maps all variables to String type - typed values not yet implemented

## Value Conversion Logic
The converter handles three types of variables:
- **NullValueImpl**: Returns `null`
- **PrimitiveTypeValueImpl**: Converts to string via `.toString()`
- **ObjectValueImpl**: Serializes to JSON using ObjectMapper
- **Unknown types**: Returns `null` with warning

## Notes
- All variables are currently stored as strings in the `value` field
- Type-specific fields (`type`, `doubleValue`, `longValue`) are not populated
- The converter includes logging for debugging variable type conversions

