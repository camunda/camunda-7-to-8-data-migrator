# EntityConversionContext Usage Examples

## Using the DbModel Approach

This document shows how to use the refactored `EntityConversionContext` with the DbModel approach.

## Example 1: Basic Flow Node Interceptor

```java
@Component
public class CustomFlowNodeInterceptor implements EntityInterceptor {
    
    @Override
    public Set<Class<?>> getEntityTypes() {
        return Set.of(HistoricActivityInstance.class);
    }
    
    @Override
    public void execute(EntityConversionContext<?, ?> context) {
        if (!(context.getC7Entity() instanceof HistoricActivityInstance activity)) {
            return;
        }
        
        // Get metadata
        Long processInstanceKey = (Long) context.getMetadata("processInstanceKey");
        Long processDefinitionKey = (Long) context.getMetadata("processDefinitionKey");
        
        // Build the complete FlowNodeInstanceDbModel
        FlowNodeInstanceDbModel dbModel = new FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder()
            .flowNodeInstanceKey(getNextKey())
            .flowNodeId(activity.getActivityId())
            .processInstanceKey(processInstanceKey)
            .processDefinitionKey(processDefinitionKey)
            .startDate(convertDate(activity.getStartTime()))
            .endDate(convertDate(activity.getEndTime()))
            .state(convertState(activity))
            .type(convertType(activity.getActivityType()))
            .tenantId(getTenantId(activity.getTenantId()))
            .partitionId(C7_HISTORY_PARTITION_ID)
            .build();
        
        // Set the model in the context
        context.setC8DbModel(dbModel);
    }
}
```

## Example 2: Modifying an Existing DbModel

If you want to add custom logic on top of the default interceptor, you can modify the existing DbModel:

```java
@Component
@Order(200)  // Run after DefaultFlowNodeConverter (which has @Order(100))
public class TreePathCalculator implements EntityInterceptor {
    
    @Override
    public Set<Class<?>> getEntityTypes() {
        return Set.of(HistoricActivityInstance.class);
    }
    
    @Override
    public void execute(EntityConversionContext<?, ?> context) {
        if (!(context.getC7Entity() instanceof HistoricActivityInstance activity)) {
            return;
        }
        
        // Get the model built by previous interceptors
        FlowNodeInstanceDbModel currentModel = (FlowNodeInstanceDbModel) context.getC8DbModel();
        
        if (currentModel == null) {
            // If no model exists yet, create one (though this shouldn't happen if DefaultFlowNodeConverter ran first)
            return;
        }
        
        // Calculate custom tree path
        String treePath = calculateTreePath(activity);
        
        // Create a new model with the updated field
        // Note: If FlowNodeInstanceDbModel has a toBuilder() method, use that
        // Otherwise, you need to create a new instance with all fields
        FlowNodeInstanceDbModel updatedModel = new FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder()
            .flowNodeInstanceKey(currentModel.flowNodeInstanceKey())
            .flowNodeId(currentModel.flowNodeId())
            .processInstanceKey(currentModel.processInstanceKey())
            .processDefinitionKey(currentModel.processDefinitionKey())
            .startDate(currentModel.startDate())
            .endDate(currentModel.endDate())
            .state(currentModel.state())
            .type(currentModel.type())
            .tenantId(currentModel.tenantId())
            .partitionId(currentModel.partitionId())
            .incidentKey(currentModel.incidentKey())
            .treePath(treePath)  // Set the custom value
            .build();
        
        context.setC8DbModel(updatedModel);
    }
    
    private String calculateTreePath(HistoricActivityInstance activity) {
        // Your custom logic here
        return "PI_" + activity.getProcessInstanceId() + "/" + activity.getId();
    }
}
```

## Example 3: Using Metadata to Share Information

Interceptors can use metadata to share information without modifying the DbModel:

```java
@Component
@Order(50)  // Run before default converters
public class MetadataEnricher implements EntityInterceptor {
    
    @Override
    public Set<Class<?>> getEntityTypes() {
        return Set.of();  // Handle all entity types
    }
    
    @Override
    public void execute(EntityConversionContext<?, ?> context) {
        // Add custom metadata that other interceptors can use
        context.setMetadata("migrationTimestamp", System.currentTimeMillis());
        context.setMetadata("migrationVersion", "1.0.0");
    }
}

@Component
@Order(200)  // Run after default converters
public class MetadataConsumer implements EntityInterceptor {
    
    @Override
    public Set<Class<?>> getEntityTypes() {
        return Set.of(HistoricActivityInstance.class);
    }
    
    @Override
    public void execute(EntityConversionContext<?, ?> context) {
        // Read metadata set by previous interceptors
        Long timestamp = (Long) context.getMetadata("migrationTimestamp");
        String version = (String) context.getMetadata("migrationVersion");
        
        // Use the metadata in your logic
        LOGGER.info("Processing entity migrated at {} with version {}", timestamp, version);
    }
}
```

## Example 4: Type-Safe Context Helper

Using the `InterceptorCompositionUtilities` for cleaner code:

```java
@Component
public class TypeSafeInterceptorExample {
    
    public static EntityInterceptor createCustomConverter() {
        return InterceptorCompositionUtilities.typeSafe(
            HistoricActivityInstance.class,
            context -> {
                HistoricActivityInstance activity = context.getEntity();  // Type-safe!
                
                // Build your DbModel
                FlowNodeInstanceDbModel dbModel = new FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder()
                    .flowNodeInstanceKey(getNextKey())
                    .flowNodeId(activity.getActivityId())
                    // ... other fields
                    .build();
                
                context.setC8DbModel(dbModel);
            }
        );
    }
}
```

## Example 5: Conditional Interceptor

Only execute logic under certain conditions:

```java
@Component
public class ConditionalInterceptorExample {
    
    @Value("${migrator.enable-custom-logic:false}")
    private boolean enableCustomLogic;
    
    @Override
    public void execute(EntityConversionContext<?, ?> context) {
        if (!enableCustomLogic) {
            return;  // Skip this interceptor
        }
        
        // Your custom logic here
    }
}
```

## Key Points to Remember

1. **DbModel is immutable**: Most DbModel classes are immutable, so you need to create a new instance to modify fields
2. **Interceptor order matters**: Use `@Order` to control execution sequence
3. **Metadata is for sharing**: Use metadata to pass information between interceptors without modifying the DbModel
4. **Type safety**: The context provides type-safe access to both C7 entities and C8 DbModels
5. **Null checks**: Always check if `getC8DbModel()` returns null before modifying

## Comparison: Old vs New

### Old Properties Approach
```java
context.setProperty("startDate", convertDate(activity.getStartTime()));
context.setProperty("endDate", convertDate(activity.getEndTime()));
```

### New DbModel Approach
```java
FlowNodeInstanceDbModel dbModel = new FlowNodeInstanceDbModelBuilder()
    .startDate(convertDate(activity.getStartTime()))
    .endDate(convertDate(activity.getEndTime()))
    .build();
context.setC8DbModel(dbModel);
```

The new approach provides:
- Compile-time type safety
- Better IDE autocomplete
- Validation at build time
- Immutability guarantees
- Clearer intent

