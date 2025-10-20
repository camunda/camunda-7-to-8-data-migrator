    // Add setter methods for Spring property binding
    public void setEnableFeature(boolean enableFeature) {
        this.enableFeature = enableFeature;
    }
    
    public void setCustomValue(String customValue) {
        this.customValue = customValue;
    }
    
    // ...rest of implementation
}
```

### 3. Configure in YAML

```yaml
camunda:
  migrator:
    entity-interceptors:
      - className: "com.example.MyCustomInterceptor"
        enabled: true
        properties:
          enableFeature: false
          customValue: "custom"
```

## Available Entity Types

You can create interceptors for these C7 historic entity types:

- `org.camunda.bpm.engine.history.HistoricProcessInstance` - Process instances
- `org.camunda.bpm.engine.history.HistoricActivityInstance` - Flow nodes/activities
- `org.camunda.bpm.engine.history.HistoricVariableInstance` - Variables
- `org.camunda.bpm.engine.history.HistoricTaskInstance` - User tasks
- `org.camunda.bpm.engine.history.HistoricIncident` - Incidents
- `org.camunda.bpm.engine.history.HistoricDecisionInstance` - Decision instances

## Execution Order

Interceptors execute in order:

1. **Built-in Converters (Order 100)**: Default conversion logic
2. **Custom Interceptors (Order 1000+)**: Your custom logic
3. **Post-processing (Order 9000+)**: Final transformations

Lower order values execute first. Interceptors with the same order execute in registration order.

## Best Practices

1. **Be Specific**: Use `getEntityTypes()` to limit your interceptor to relevant entity types
2. **Set Appropriate Order**: Use order > 100 to override built-in converters
3. **Handle Nulls**: Always check for null values before processing
4. **Log Important Actions**: Use logging for debugging and audit trails
5. **Use Metadata**: Pass additional context through metadata rather than global state
6. **Test Thoroughly**: Test with various entity states and edge cases

## Troubleshooting

**Interceptor not executing:**
- Check that the JAR is in `configuration/userlib`
- Verify the className in application.yml matches exactly
- Ensure `enabled: true` is set
- Check logs for registration errors

**Properties not being set:**
- Verify setter methods exist and are public
- Check property names match exactly (case-sensitive)
- Review logs for binding errors

**Wrong values in C8:**
- Check interceptor order - later interceptors override earlier ones
- Verify property names match C8 model fields
- Use logging to debug property values
# Entity Interceptor Example

This example demonstrates how to create custom entity interceptors for the History Data Migrator.

## Overview

Entity interceptors allow you to customize how C7 historic entities are converted to C8 database models. This is similar to variable interceptors but works at the entity level rather than variable level.

## Use Cases

- **Custom Property Calculation**: Calculate complex properties like `treePath` that aren't available in C7
- **Property Override**: Override default conversion logic for specific properties
- **Property Nullification**: Explicitly set properties to null based on business rules
- **Validation**: Add custom validation logic during entity conversion
- **Audit Logging**: Log entity conversions for audit purposes

## Included Examples

### 1. ProcessInstanceTreePathInterceptor

This interceptor demonstrates:
- Type-specific interceptor (only processes `HistoricProcessInstance`)
- Overriding a specific property (`treePath`)
- Configurable properties via YAML
- Custom business logic

**File**: `ProcessInstanceTreePathInterceptor.java`

## How to Use

### Step 1: Build the JAR

```bash
cd examples/variable-interceptor
mvn clean package
```

This creates `target/my-variable-interceptor-0.2.0-SNAPSHOT.jar`

### Step 2: Deploy the JAR

Copy the JAR to the migrator's `configuration/userlib` folder:

```bash
copy target\my-variable-interceptor-0.2.0-SNAPSHOT.jar ..\..\assembly\resources\configuration\userlib\
```

### Step 3: Configure in application.yml

Add to `configuration/application.yml`:

```yaml
camunda:
  migrator:
    entity-interceptors:
      # Custom interceptor for calculating treePath
      - className: "io.camunda.migrator.example.ProcessInstanceTreePathInterceptor"
        enabled: true
        properties:
          enableLogging: true
          pathSeparator: "/"
      
      # You can disable built-in interceptors if needed
      - className: "io.camunda.migrator.impl.interceptor.DefaultProcessInstanceConverter"
        enabled: false  # Disable default conversion logic
```

## Creating Your Own Interceptor

### 1. Create a New Class

```java
package com.example;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

public class MyCustomInterceptor implements EntityInterceptor {

    @Override
    public Set<Class<?>> getEntityTypes() {
        // Specify which entity types this interceptor handles
        return Set.of(HistoricProcessInstance.class);
        // Or return Set.of() to handle all entity types
    }

    @Override
    public int getOrder() {
        // Lower values execute first
        // Built-in converters use 100
        // Custom interceptors typically use 1000-5000
        return 1000;
    }

    @Override
    public void execute(EntityConversionContext context) {
        // Access the C7 entity
        HistoricProcessInstance pi = (HistoricProcessInstance) context.getC7Entity();
        
        // Read existing property values
        String tenantId = (String) context.getProperty("tenantId");
        
        // Override a property
        context.setProperty("treePath", calculateTreePath(pi));
        
        // Nullify a property
        context.nullifyProperty("parentElementInstanceKey");
        
        // Access metadata passed from converter
        Long pdKey = (Long) context.getMetadata("processDefinitionKey");
    }
    
    private String calculateTreePath(HistoricProcessInstance pi) {
        // Your custom logic here
        return "custom-path";
    }
}
```

### 2. Add Configurable Properties (Optional)

```java
public class MyCustomInterceptor implements EntityInterceptor {
    private boolean enableFeature = true;
    private String customValue = "default";
    
/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.example;

import io.camunda.migrator.interceptor.EntityConversionContext;
import io.camunda.migrator.interceptor.EntityInterceptor;
import java.util.Set;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example entity interceptor that calculates the treePath property for process instances.
 * <p>
 * This demonstrates:
 * - How to create a custom entity interceptor for a specific entity type
 * - How to override a specific property (treePath)
 * - How to access the C7 entity and metadata
 * - How to implement custom business logic
 * </p>
 *
 * <h2>Configuration Example:</h2>
 * <pre>
 * camunda:
 *   migrator:
 *     entity-interceptors:
 *       - className: "io.camunda.migrator.example.ProcessInstanceTreePathInterceptor"
 *         enabled: true
 *         properties:
 *           enableLogging: true
 *           pathSeparator: "/"
 * </pre>
 */
public class ProcessInstanceTreePathInterceptor implements EntityInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceTreePathInterceptor.class);

  // Configurable properties
  private boolean enableLogging = true;
  private String pathSeparator = "/";

  @Override
  public Set<Class<?>> getEntityTypes() {
    // Only handle HistoricProcessInstance entities
    return Set.of(HistoricProcessInstance.class);
  }

  @Override
  public int getOrder() {
    // Run after default converter (order 100) to override treePath
    return 1000;
  }

  @Override
  public void execute(EntityConversionContext context) {
    if (!(context.getC7Entity() instanceof HistoricProcessInstance processInstance)) {
      return;
    }

    if (enableLogging) {
      LOGGER.info("Calculating treePath for process instance: {}", processInstance.getId());
    }

    // Calculate treePath based on custom logic
    String treePath = calculateTreePath(context, processInstance);
    
    // Override the treePath property
    context.setProperty("treePath", treePath);

    if (enableLogging) {
      LOGGER.info("Set treePath to: {}", treePath);
    }
  }

  /**
   * Custom logic to calculate the tree path for a process instance.
   * This example creates a simple hierarchical path based on parent relationships.
   *
   * @param context the conversion context
   * @param processInstance the C7 process instance
   * @return the calculated tree path
   */
  private String calculateTreePath(EntityConversionContext<HistoricProcessInstance> context, 
                                    HistoricProcessInstance processInstance) {
    Long processInstanceKey = (Long) context.getProperty("processInstanceKey");
    Long parentProcessInstanceKey = (Long) context.getMetadata("parentProcessInstanceKey");

    if (parentProcessInstanceKey == null) {
      // Root process instance
      return String.valueOf(processInstanceKey) + pathSeparator;
    } else {
      // Child process instance - build hierarchical path
      // In a real implementation, you might need to look up the parent's treePath
      return parentProcessInstanceKey + pathSeparator + processInstanceKey + pathSeparator;
    }
  }

  // Setter methods for configuration properties
  public void setEnableLogging(boolean enableLogging) {
    this.enableLogging = enableLogging;
  }

  public void setPathSeparator(String pathSeparator) {
    this.pathSeparator = pathSeparator;
  }

  // Getter methods
  public boolean isEnableLogging() {
    return enableLogging;
  }

  public String getPathSeparator() {
    return pathSeparator;
  }
}

