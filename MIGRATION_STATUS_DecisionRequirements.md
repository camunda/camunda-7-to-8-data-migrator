# DecisionRequirements Model - Migration Status

## Model Information
- **C8 DbModel Class**: `DecisionRequirementsDbModel`
- **Converter Class**: `DecisionRequirementsDefinitionConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| decisionRequirementsKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| decisionRequirementsId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `DecisionRequirementsDefinition.getKey()` |
| name | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `DecisionRequirementsDefinition.getName()` |
| resourceName | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `DecisionRequirementsDefinition.getResourceName()` |
| version | Integer | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `DecisionRequirementsDefinition.getVersion()` |
| xml | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | Set to `null` - TODO: not stored in C7 DecisionRequirementsDefinition |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `DecisionRequirementsDefinition.getTenantId()` via `getTenantId()` |

## Summary Statistics
- **Total Fields**: 7
- **Migrated**: 6 (85.7%)
- **Set to Null (Nullify)**: 1 (14.3%)
- **Has TODO**: 1 (14.3%)
- **Needs Implementation**: 1 (14.3%)
- **Can Be Migrated**: 6 (85.7%)
- **Permanently Null (not in C7)**: 1 (14.3%)
- **Fully Migrated (non-null)**: 6 (85.7%)

## Outstanding Issues
1. **xml**: DMN XML content - TODO: not stored in C7 DecisionRequirementsDefinition
   - This field contains the Decision Model and Notation (DMN) XML
   - C7's `DecisionRequirementsDefinition` does not expose this directly
   - Would need to be fetched from deployment resources similar to BPMN XML

## Notes
- Nearly complete migration (85.7%)
- DecisionRequirements is a repository/definition entity, not a history entity
- The missing XML field is similar to ProcessDefinition's bpmnXml, but lacks implementation
- Could potentially be implemented by fetching from deployment resources using the resourceName

