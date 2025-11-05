# Database Model Fields Reference

This document lists all field names and types for each database model in the Camunda 8 RDBMS schema.

---

## AuthorizationDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| authorizationKey | Long |
| ownerId | String |
| ownerType | String |
| resourceType | String |
| resourceMatcher | Short |
| resourceId | String |
| permissionTypes | Set<PermissionType> |

---

## BatchOperationDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| batchOperationKey | String |
| state | BatchOperationState |
| operationType | BatchOperationType |
| startDate | OffsetDateTime |
| endDate | OffsetDateTime |
| operationsTotalCount | Integer |
| operationsFailedCount | Integer |
| operationsCompletedCount | Integer |
| errors | List<BatchOperationErrorDbModel> |

---

## BatchOperationItemDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| batchOperationKey | String |
| itemKey | long |
| processInstanceKey | long |
| state | BatchOperationItemState |
| processedDate | OffsetDateTime |
| errorMessage | String |

---

## CorrelatedMessageSubscriptionDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| correlationKey | String |
| correlationTime | OffsetDateTime |
| flowNodeId | String |
| flowNodeInstanceKey | Long |
| historyCleanupDate | OffsetDateTime |
| messageKey | Long |
| messageName | String |
| partitionId | Integer |
| processDefinitionId | String |
| processDefinitionKey | Long |
| processInstanceKey | Long |
| subscriptionKey | Long |
| subscriptionType | MessageSubscriptionType |
| tenantId | String |

---

## DecisionDefinitionDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| decisionDefinitionKey | Long |
| name | String |
| decisionDefinitionId | String |
| tenantId | String |
| version | int |
| decisionRequirementsId | String |
| decisionRequirementsKey | Long |

---

## DecisionInstanceDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| decisionInstanceId | String |
| decisionInstanceKey | Long |
| state | DecisionInstanceState |
| evaluationDate | OffsetDateTime |
| evaluationFailure | String |
| evaluationFailureMessage | String |
| result | String |
| flowNodeInstanceKey | Long |
| flowNodeId | String |
| processInstanceKey | Long |
| processDefinitionKey | Long |
| processDefinitionId | String |
| decisionDefinitionKey | Long |
| decisionDefinitionId | String |
| decisionRequirementsKey | Long |
| decisionRequirementsId | String |
| rootDecisionDefinitionKey | Long |
| decisionType | DecisionDefinitionType |
| tenantId | String |
| partitionId | int |
| evaluatedInputs | List<EvaluatedInput> |
| evaluatedOutputs | List<EvaluatedOutput> |
| historyCleanupDate | OffsetDateTime |

### Nested: EvaluatedInput
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| decisionInstanceId | String |
| id | String |
| name | String |
| value | String |

### Nested: EvaluatedOutput
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| decisionInstanceId | String |
| id | String |
| name | String |
| value | String |
| ruleId | String |
| ruleIndex | Integer |

---

## DecisionRequirementsDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| decisionRequirementsKey | Long |
| decisionRequirementsId | String |
| name | String |
| resourceName | String |
| version | Integer |
| xml | String |
| tenantId | String |

---

## ExporterPositionModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| partitionId | int |
| exporter | String |
| lastExportedPosition | Long |
| created | LocalDateTime |
| lastUpdated | LocalDateTime |

---

## FlowNodeInstanceDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| flowNodeInstanceKey | Long |
| processInstanceKey | Long |
| processDefinitionKey | Long |
| processDefinitionId | String |
| flowNodeScopeKey | Long |
| startDate | OffsetDateTime |
| endDate | OffsetDateTime |
| flowNodeId | String |
| flowNodeName | String |
| treePath | String |
| type | FlowNodeType |
| state | FlowNodeState |
| incidentKey | Long |
| numSubprocessIncidents | Long |
| hasIncident | Boolean |
| tenantId | String |
| partitionId | Integer |
| historyCleanupDate | OffsetDateTime |

---

## FormDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| formKey | Long |
| tenantId | String |
| formId | String |
| schema | String |
| version | Long |
| isDeleted | Boolean |

---

## GroupDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| groupKey | Long |
| groupId | String |
| name | String |
| description | String |
| members | List<GroupMemberDbModel> |

---

## GroupMemberDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| groupId | String |
| entityId | String |
| entityType | String |

---

## IncidentDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| incidentKey | Long |
| processDefinitionKey | Long |
| processDefinitionId | String |
| processInstanceKey | Long |
| flowNodeInstanceKey | Long |
| flowNodeId | String |
| jobKey | Long |
| errorType | ErrorType |
| errorMessage | String |
| errorMessageHash | Integer |
| creationDate | OffsetDateTime |
| state | IncidentState |
| treePath | String |
| tenantId | String |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

---

## JobDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| jobKey | Long |
| type | String |
| worker | String |
| state | JobState |
| kind | JobKind |
| listenerEventType | ListenerEventType |
| retries | Integer |
| isDenied | Boolean |
| deniedReason | String |
| hasFailedWithRetriesLeft | Boolean |
| errorCode | String |
| errorMessage | String |
| serializedCustomHeaders | String |
| customHeaders | Map<String, String> |
| deadline | OffsetDateTime |
| endTime | OffsetDateTime |
| processDefinitionId | String |
| processDefinitionKey | Long |
| processInstanceKey | Long |
| elementId | String |
| elementInstanceKey | Long |
| tenantId | String |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

---

## MappingRuleDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| mappingRuleId | String |
| mappingRuleKey | Long |
| claimName | String |
| claimValue | String |
| name | String |

---

## MessageSubscriptionDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| messageSubscriptionKey | Long |
| processDefinitionId | String |
| processDefinitionKey | Long |
| processInstanceKey | Long |
| flowNodeId | String |
| flowNodeInstanceKey | Long |
| messageSubscriptionState | MessageSubscriptionState |
| dateTime | OffsetDateTime |
| messageName | String |
| correlationKey | String |
| tenantId | String |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

---

## ProcessDefinitionDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| processDefinitionKey | Long |
| processDefinitionId | String |
| resourceName | String |
| name | String |
| tenantId | String |
| versionTag | String |
| version | int |
| bpmnXml | String |
| formId | String |

---

## ProcessInstanceDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| processInstanceKey | Long |
| processDefinitionId | String |
| processDefinitionKey | Long |
| state | ProcessInstanceState |
| startDate | OffsetDateTime |
| endDate | OffsetDateTime |
| tenantId | String |
| parentProcessInstanceKey | Long |
| parentElementInstanceKey | Long |
| numIncidents | Integer |
| version | int |
| partitionId | int |
| treePath | String |
| historyCleanupDate | OffsetDateTime |

---

## ProcessInstanceTagDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| processInstanceKey | Long |
| tagValue | String |

---

## RoleDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| roleKey | Long |
| roleId | String |
| name | String |
| description | String |
| members | List<RoleMemberDbModel> |

---

## RoleMemberDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| roleId | String |
| entityId | String |
| entityType | String |

---

## SequenceFlowDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| flowNodeId | String |
| processInstanceKey | Long |
| processDefinitionKey | Long |
| processDefinitionId | String |
| tenantId | String |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

**Note:** Also has computed field `sequenceFlowId()` = `{processInstanceKey}_{flowNodeId}`

---

## TenantDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| tenantKey | Long |
| tenantId | String |
| name | String |
| description | String |
| members | List<TenantMemberDbModel> |

---

## TenantMemberDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| tenantId | String |
| entityId | String |
| entityType | String |

---

## UsageMetricDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| key | long |
| startTime | OffsetDateTime |
| endTime | OffsetDateTime |
| tenantId | String |
| eventType | EventTypeDbModel |
| value | Long |
| partitionId | int |

**Note:** Also has computed field `getId()` = `{key}_{tenantId}`

### Nested: UsageMetricStatisticsDbModel
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| rpi | Long |
| edi | Long |
| at | Long |

### Nested: UsageMetricTenantStatisticsDbModel
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| tenantId | String |
| rpi | Long |
| edi | Long |

### Enum: EventTypeDbModel
- RPI(0)
- EDI(1)
- TU(2)

---

## UsageMetricTUDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| key | long |
| startTime | OffsetDateTime |
| endTime | OffsetDateTime |
| tenantId | String |
| assigneeHash | long |
| partitionId | int |

**Note:** Also has computed field `getId()` = `{key}_{tenantId}`

### Nested: UsageMetricTUStatisticsDbModel
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| tu | Long |

### Nested: UsageMetricTUTenantStatisticsDbModel
| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| tenantId | String |
| tu | Long |

---

## UserDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| userKey | Long |
| username | String |
| name | String |
| email | String |
| password | String |

---

## UserTaskDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| userTaskKey | Long |
| elementId | String |
| name | String |
| processDefinitionId | String |
| creationDate | OffsetDateTime |
| completionDate | OffsetDateTime |
| assignee | String |
| state | UserTaskState |
| formKey | Long |
| processDefinitionKey | Long |
| processInstanceKey | Long |
| elementInstanceKey | Long |
| tenantId | String |
| dueDate | OffsetDateTime |
| followUpDate | OffsetDateTime |
| candidateGroups | List<String> |
| candidateUsers | List<String> |
| externalFormReference | String |
| processDefinitionVersion | Integer |
| serializedCustomHeaders | String |
| customHeaders | Map<String, String> |
| priority | Integer |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

---

## UserTaskMigrationDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| userTaskKey | Long |
| processDefinitionKey | Long |
| processDefinitionId | String |
| elementId | String |
| name | String |
| processDefinitionVersion | Integer |

---

## VariableDbModel

| Field Name | Type | Needs Implementation | Nullify | Can Be Migrated  | Needs Revert to Nullify |
|------------|------|------|------|------|------|
| variableKey | Long |
| name | String |
| type | ValueTypeEnum |
| doubleValue | Double |
| longValue | Long |
| value | String |
| fullValue | String |
| isPreview | boolean |
| scopeKey | Long |
| processInstanceKey | Long |
| processDefinitionId | String |
| tenantId | String |
| partitionId | int |
| historyCleanupDate | OffsetDateTime |

---

## Summary

**Total Models**: 32

**Models by Category**:
- **History/Process Models**: ProcessInstance, ProcessDefinition, FlowNodeInstance, Variable, UserTask, Incident, DecisionInstance, DecisionDefinition, DecisionRequirements, Job, MessageSubscription, CorrelatedMessageSubscription, SequenceFlow
- **Identity/Authorization Models**: User, Group, GroupMember, Role, RoleMember, Tenant, TenantMember, Authorization, MappingRule
- **Operational Models**: BatchOperation, BatchOperationItem, Form, ProcessInstanceTag, UserTaskMigration
- **Metrics Models**: UsageMetric, UsageMetricTU
- **System Models**: ExporterPosition

