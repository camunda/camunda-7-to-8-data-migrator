# ProcessDefinition Model - Migration Status

## Model Information
- **C8 DbModel Class**: `ProcessDefinitionDbModel`
- **Converter Class**: `ProcessDefinitionConverter`
- **Has Converter**: ✅ Yes

## Field Migration Status

| Field Name | Type | Has Converter | Is Migrated | Is Null | Has TODO | Needs Implementation | Nullify | Can Be Migrated | Needs Revert to Nullify | Notes |
|------------|------|---------------|-------------|---------|----------|---------------------|---------|-----------------|------------------------|-------|
| processDefinitionKey | Long | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | Generated via `getNextKey()` |
| processDefinitionId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getKey()` |
| resourceName | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getResourceName()` |
| name | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getName()` |
| tenantId | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getTenantId()` |
| versionTag | String | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getVersionTag()` |
| version | int | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | From `ProcessDefinition.getVersion()` |
| bpmnXml | String | ✅ | ✅ | ❓ | ❌ | ❌ | ❌ | ✅ | ❌ | Fetched from deployment resource via `C7Client.getResourceAsStream()` |
| formId | String | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ❓ | ❌ | Set to `null` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347 |

## Summary Statistics
- **Total Fields**: 9
- **Migrated**: 8 (88.9%)
- **Set to Null (Nullify)**: 1 (11.1%)
- **Has TODO**: 1 (11.1%)
- **Needs Implementation**: 1 (11.1%)
- **Can Be Migrated**: 8 (88.9%)
- **Permanently Null (not in C7)**: 0 (0%)
- **Fully Migrated (non-null)**: 8 (88.9%)

## Outstanding Issues
1. **formId**: Form ID information - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347

## Special Processing
### BPMN XML Retrieval
The converter includes special logic to fetch the BPMN XML:
1. Uses `C7Client.getResourceAsStream()` with deployment ID and resource name
2. Reads the input stream to a string using UTF-8 encoding
3. Returns `null` if IOException occurs (with logging)

## Notes
- The converter has error handling for BPMN XML retrieval failures
- ProcessDefinition is a repository/definition entity, not a history entity
- High migration rate (88.9%) - only formId is missing

