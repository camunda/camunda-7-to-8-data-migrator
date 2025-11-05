# Default Entity Interceptors Summary

## Overview
Created default EntityInterceptor implementations for all Converter classes in the `io.camunda.migrator.converter` package. These interceptors follow the new DbModel-based approach.

## Created Interceptors

### 1. DefaultProcessDefinitionConverter
- **Entity Type**: `ProcessDefinition`
- **DbModel**: `ProcessDefinitionDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultProcessDefinitionConverter`
- **Dependencies**: `C7Client` (for fetching BPMN XML)
- **Metadata**: None required
- **Features**:
  - Fetches and includes BPMN XML from C7
  - Handles resource stream errors gracefully

### 2. DefaultDecisionDefinitionConverter
- **Entity Type**: `DecisionDefinition`
- **DbModel**: `DecisionDefinitionDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultDecisionDefinitionConverter`
- **Metadata Required**:
  - `decisionRequirementsKey` (Long)

### 3. DefaultDecisionRequirementsDefinitionConverter
- **Entity Type**: `DecisionRequirementsDefinition`
- **DbModel**: `DecisionRequirementsDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultDecisionRequirementsDefinitionConverter`
- **Metadata**: None required
- **Note**: XML is not stored in C7 DecisionRequirementsDefinition (set to null)

### 4. DefaultDecisionInstanceConverter
- **Entity Type**: `HistoricDecisionInstance`
- **DbModel**: `DecisionInstanceDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultDecisionInstanceConverter`
- **Metadata Required**:
  - `decisionDefinitionKey` (Long)
  - `processDefinitionKey` (Long)
  - `decisionRequirementsDefinitionKey` (Long)
  - `processInstanceKey` (Long)
  - `rootDecisionDefinitionKey` (Long)
  - `flowNodeInstanceKey` (Long)
  - `flowNodeId` (String)
- **Features**:
  - Maps evaluated inputs and outputs
  - Formats decision instance ID

### 5. DefaultIncidentConverter
- **Entity Type**: `HistoricIncident`
- **DbModel**: `IncidentDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultIncidentConverter`
- **Metadata Required**:
  - `processDefinitionKey` (Long)
  - `processInstanceKey` (Long)
  - `jobDefinitionKey` (Long)
  - `flowNodeInstanceKey` (Long)
- **Features**:
  - Converts incident state
  - TODO: Error type not available in C7
  - TODO: Incident state needs to be made accessible

### 6. DefaultVariableConverter
- **Entity Type**: `HistoricVariableInstance`
- **DbModel**: `VariableDbModel`
- **Location**: `io.camunda.migrator.impl.interceptor.DefaultVariableConverter`
- **Dependencies**: `ObjectMapper` (for JSON serialization)
- **Metadata Required**:
  - `processInstanceKey` (Long)
  - `scopeKey` (Long)
- **Features**:
  - Handles null values
  - Handles primitive types
  - Handles object types (converts to JSON)
  - Logging for different value types

## Existing Interceptors (Already Created)

The following interceptors were already created in the previous refactoring:

### 7. DefaultFlowNodeConverter
- **Entity Type**: `HistoricActivityInstance`
- **DbModel**: `FlowNodeInstanceDbModel`
- **Metadata Required**: `processInstanceKey`, `processDefinitionKey`

### 8. DefaultProcessInstanceConverter
- **Entity Type**: `HistoricProcessInstance`
- **DbModel**: `ProcessInstanceDbModel`
- **Metadata Required**: `processDefinitionKey`, `parentProcessInstanceKey`

### 9. DefaultUserTaskConverter
- **Entity Type**: `HistoricTaskInstance`
- **DbModel**: `UserTaskDbModel`
- **Metadata Required**: `processInstanceKey`, `flowNodeInstanceKey`, `processDefinitionKey`, `processDefinitionVersion`

## Implementation Pattern

All interceptors follow this pattern:

```java
@Component
public class DefaultXxxConverter implements EntityInterceptor {

  @Override
  public Set<Class<?>> getEntityTypes() {
    return Set.of(XxxEntity.class);
  }

  @Override
  public void execute(EntityConversionContext<?, ?> context) {
    if (!(context.getC7Entity() instanceof XxxEntity entity)) {
      return;
    }

    // Extract metadata
    Long someKey = (Long) context.getMetadata("someKey");

    // Build DbModel
    XxxDbModel dbModel = new XxxDbModel.Builder()
        .field1(value1)
        .field2(value2)
        .build();

    // Set in context with proper type casting
    @SuppressWarnings("unchecked")
    EntityConversionContext<XxxEntity, XxxDbModel> typedContext =
        (EntityConversionContext<XxxEntity, XxxDbModel>) context;
    typedContext.setC8DbModel(dbModel);
  }
}
```

## Type Safety

Due to the generic wildcard signature `EntityConversionContext<?, ?>` in the `EntityInterceptor.execute()` method, we need to perform an unchecked cast to set the DbModel. This is safe because:

1. The interceptor only executes for the specific entity type (enforced by `getEntityTypes()`)
2. The DbModel matches the entity type
3. The cast is properly suppressed with `@SuppressWarnings("unchecked")`

## Integration

All interceptors are:
- Annotated with `@Component` for Spring auto-discovery
- Automatically registered with the `EntityConversionService`
- Executed in order when converting entities
- Can be disabled via configuration

## Benefits

1. **Consistency**: All converters now use the same EntityInterceptor pattern
2. **Extensibility**: Users can add custom interceptors or disable default ones
3. **Type Safety**: Full compile-time type checking with DbModel classes
4. **Testability**: Each interceptor can be tested independently
5. **Maintainability**: Clear separation of concerns

## Next Steps

The original Converter classes in the `io.camunda.migrator.converter` package can now be considered deprecated in favor of these EntityInterceptor implementations. They can be:
- Kept for backward compatibility
- Removed if not needed
- Refactored to use the EntityConversionService internally

