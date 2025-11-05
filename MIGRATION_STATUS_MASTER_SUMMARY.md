# Migration Status - Master Summary

## Overview
This document provides a comprehensive overview of the migration status for all Camunda 7 to Camunda 8 database models in the History Data Migrator project.

**Generated Date**: October 31, 2025

---

## Models Summary

| Model | Total Fields | Migrated | Null/Missing | Has Converter | Migration % | Status |
|-------|--------------|----------|--------------|---------------|-------------|--------|
| [DecisionDefinition](#decisiondefinition) | 7 | 7 | 0 | ✅ | 100% | ✅ Complete |
| [ProcessDefinition](#processdefinition) | 9 | 8 | 1 | ✅ | 88.9% | ⚠️ Near Complete |
| [DecisionRequirements](#decisionrequirements) | 7 | 6 | 1 | ✅ | 85.7% | ⚠️ Near Complete |
| [DecisionInstance](#decisioninstance) | 23 | 19 | 4 | ✅ | 82.6% | ⚠️ Good |
| [ProcessInstance](#processinstance) | 14 | 11 | 3 | ✅ | 78.6% | ⚠️ Good |
| [UserTask](#usertask) | 23 | 18 | 5 | ✅ | 78.3% | ⚠️ Good |
| [Incident](#incident) | 16 | 10 | 6 | ✅ | 62.5% | ⚠️ Needs Work |
| [Variable](#variable) | 14 | 8 | 6 | ✅ | 57.1% | ⚠️ Needs Work |
| [FlowNodeInstance](#flownodeinstance) | 18 | 8 | 10 | ✅ | 44.4% | ❌ Needs Work |

**Overall Statistics**:
- **Total Fields Across All Models**: 131
- **Successfully Migrated**: 95 (72.5%)
- **Null/Missing**: 36 (27.5%)
- **Models with TODOs**: 8 of 9

---

## Model Details

### DecisionDefinition
**Migration: 100% ✅ COMPLETE**
- **File**: [MIGRATION_STATUS_DecisionDefinition.md](MIGRATION_STATUS_DecisionDefinition.md)
- **Converter**: `DecisionDefinitionConverter`
- **Outstanding Issues**: None
- **Notes**: Perfect migration - all fields successfully mapped

### ProcessDefinition
**Migration: 88.9% ⚠️ NEAR COMPLETE**
- **File**: [MIGRATION_STATUS_ProcessDefinition.md](MIGRATION_STATUS_ProcessDefinition.md)
- **Converter**: `ProcessDefinitionConverter`
- **Outstanding Issues**:
  - `formId` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347

### DecisionRequirements
**Migration: 85.7% ⚠️ NEAR COMPLETE**
- **File**: [MIGRATION_STATUS_DecisionRequirements.md](MIGRATION_STATUS_DecisionRequirements.md)
- **Converter**: `DecisionRequirementsDefinitionConverter`
- **Outstanding Issues**:
  - `xml` - Not stored in C7 DecisionRequirementsDefinition (could be fetched from deployment)

### DecisionInstance
**Migration: 82.6% ⚠️ GOOD**
- **File**: [MIGRATION_STATUS_DecisionInstance.md](MIGRATION_STATUS_DecisionInstance.md)
- **Converter**: `DecisionInstanceConverter`
- **Outstanding Issues**:
  - `state` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370
  - `decisionType` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5370
  - `evaluationFailure` - Not stored in C7
  - `evaluationFailureMessage` - Not stored in C7

### ProcessInstance
**Migration: 78.6% ⚠️ GOOD**
- **File**: [MIGRATION_STATUS_ProcessInstance.md](MIGRATION_STATUS_ProcessInstance.md)
- **Converter**: `ProcessInstanceConverter`
- **Outstanding Issues**:
  - `parentElementInstanceKey` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5359
  - `numIncidents` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5400
  - `treePath` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5359

### UserTask
**Migration: 78.3% ⚠️ GOOD**
- **File**: [MIGRATION_STATUS_UserTask.md](MIGRATION_STATUS_UserTask.md)
- **Converter**: `UserTaskConverter`
- **Outstanding Issues**:
  - `formKey` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5347
  - `candidateGroups` - TODO: ?
  - `candidateUsers` - TODO: ?
  - `externalFormReference` - TODO: ?
  - `customHeaders` - TODO: ?

### Incident
**Migration: 62.5% ⚠️ NEEDS WORK**
- **File**: [MIGRATION_STATUS_Incident.md](MIGRATION_STATUS_Incident.md)
- **Converter**: `IncidentConverter`
- **Critical Issues**:
  - `state` - Hardcoded to 0 (ACTIVE), needs proper implementation
  - `errorType` - TODO: does error type exist in C7?
  - `treePath` - TODO: ?
  - `partitionId` - **Missing from converter** (should be set)
  - `historyCleanupDate` - **Missing from converter** (should be set)
  - `errorMessageHash` - Not set (C8 specific)

### Variable
**Migration: 57.1% ⚠️ NEEDS WORK**
- **File**: [MIGRATION_STATUS_Variable.md](MIGRATION_STATUS_Variable.md)
- **Converter**: `VariableConverter`
- **Outstanding Issues**:
  - `value` - TODO: https://github.com/camunda/camunda-bpm-platform/issues/5329
  - `type` - Not set (all mapped to String)
  - `doubleValue` - Not set (all mapped to String)
  - `longValue` - Not set (all mapped to String)
  - `fullValue` - Only set when truncated
  - `isPreview` - Only set when truncated

### FlowNodeInstance
**Migration: 44.4% ❌ NEEDS WORK**
- **File**: [MIGRATION_STATUS_FlowNodeInstance.md](MIGRATION_STATUS_FlowNodeInstance.md)
- **Converter**: `FlowNodeConverter`
- **Critical Issues**:
  - `state` - TODO: Inherited from process instance
  - `treePath` - TODO: Not in C7, not supported by C8 RDBMS yet
  - `incidentKey` - TODO: Not in C7
  - `numSubprocessIncidents` - TODO: C8 specific
  - `flowNodeScopeKey` - **Missing from converter**
  - `flowNodeName` - **Missing from converter**
  - `hasIncident` - **Missing from converter**
  - `partitionId` - **Missing from converter** (should be set)
  - `historyCleanupDate` - **Missing from converter** (should be set)

---

## Priority Issues

### High Priority (Blocking)
1. **Incident.state** - Currently hardcoded, needs proper state detection
2. **Incident.partitionId** - Missing from converter
3. **Incident.historyCleanupDate** - Missing from converter
4. **FlowNodeInstance.partitionId** - Missing from converter
5. **FlowNodeInstance.historyCleanupDate** - Missing from converter

### Medium Priority (Impacting Quality)
1. **Variable type system** - All variables stored as strings, no type differentiation
2. **FlowNodeInstance missing fields** - Multiple fields not set
3. **UserTask candidate/form fields** - Multiple TODO items
4. **TreePath fields** - Multiple models missing tree path information

### Low Priority (Enhancement)
1. **DecisionRequirements.xml** - Could be fetched from deployment
2. **Form-related fields** - Waiting on C7 platform issue #5347

---

## GitHub Issues Referenced

| Issue | Description | Affects Models |
|-------|-------------|----------------|
| [#5370](https://github.com/camunda/camunda-bpm-platform/issues/5370) | Decision instance state/type | DecisionInstance |
| [#5359](https://github.com/camunda/camunda-bpm-platform/issues/5359) | Call activity instance id | ProcessInstance, FlowNodeInstance |
| [#5400](https://github.com/camunda/camunda-bpm-platform/issues/5400) | Number of incidents | ProcessInstance |
| [#5347](https://github.com/camunda/camunda-bpm-platform/issues/5347) | Form key/ID | ProcessDefinition, UserTask |
| [#5329](https://github.com/camunda/camunda-bpm-platform/issues/5329) | Variable value conversion | Variable |

---

## Recommendations

### Immediate Actions
1. **Fix missing partitionId and historyCleanupDate** in Incident and FlowNodeInstance converters
2. **Implement proper state detection** for Incident converter
3. **Add missing basic fields** to FlowNodeInstance converter (flowNodeScopeKey, flowNodeName, hasIncident)

### Short-term Actions
1. Review and implement Variable type system (type, doubleValue, longValue)
2. Investigate and implement UserTask candidate groups/users
3. Address tree path requirements across multiple models

### Long-term Actions
1. Monitor and implement GitHub issue resolutions as they become available
2. Consider fetching DecisionRequirements XML from deployment resources
3. Implement incident counting and subprocess incident logic

---

## Column Definitions

### Table Columns Explained
- **Has Converter**: Whether a converter class exists for this model
- **Is Migrated**: Whether the field is actually populated with data from C7
- **Is Null**: Whether the field is set to null in the converter
- **Has TODO**: Whether there's a TODO comment in the converter code
- **Needs Implementation**: Field needs code implementation (has TODO or is critical)
- **Nullify**: Field is intentionally set to null (not available in C7 or TODO)
- **Can Be Migrated**: Field can be successfully migrated from C7 data
- **Needs Revert to Nullify**: Field that was previously migrated needs to be changed back to null

---

## Files Generated
1. `MIGRATION_STATUS_DecisionInstance.md`
2. `MIGRATION_STATUS_ProcessInstance.md`
3. `MIGRATION_STATUS_Variable.md`
4. `MIGRATION_STATUS_UserTask.md`
5. `MIGRATION_STATUS_Incident.md`
6. `MIGRATION_STATUS_FlowNodeInstance.md`
7. `MIGRATION_STATUS_ProcessDefinition.md`
8. `MIGRATION_STATUS_DecisionDefinition.md`
9. `MIGRATION_STATUS_DecisionRequirements.md`
10. `MIGRATION_STATUS_MASTER_SUMMARY.md` (this file)

