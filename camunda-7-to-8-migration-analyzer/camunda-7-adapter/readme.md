# Spring Boot Adapter to re-use Java Delegates, Delegate Expressions, Expressions or External Tasks from Camunda Platform 7 in Camunda Platform 8

This library allows to reuse Java delegates, Delegate Expressions, Expressions
or External Tasks from process solutions developed for Camunda Platform 7 (with
Spring/Spring Boot) within Camunda Platform 8.

The adapter requires to use Spring Boot.

Details on how service tasks are adapted are described in this
[migration guide](https://docs.camunda.io/docs/guides/migrating-from-Camunda-Platform/#migration-tooling).

**Important note:\*** This adapter does not aim to cover every possible
situation, but it might work out-of-the-box for some cases or give you
jump-start to extend it to suite your needs.

# How to use

## Add dependency

Add the dependency to the adapter library (double-check for the latest version):

```xml

<dependency>
  <groupId>org.camunda.community.migration</groupId>
  <artifactId>camunda-7-adapter</artifactId>
  <version>0.4.4</version>
</dependency>
```

## Import adapter

Import the adapter into your Spring Boot application as shown in the
[example application](../example/process-solution-migrated/src/main/java/org/camunda/community/migration/example/Application.java):

```java

@SpringBootApplication
@EnableZeebeClient
@EnableCamunda7Adapter
@ZeebeDeployment(resources = "classpath:*.bpmn")
public class Application {
  // start off here
}
```

This will start a job worker that subscribes to `camunda-7-adapter` as well as
workers for each `@ExternalTaskSubscription` with Zeebe Task Type equal to
External Task Topic Name.

## Using migration worker

To use that worker, add the `taskType="camunda-7-adapter"` to your service task
and add task headers for a java delegate class, delegate expression or
expression, e.g.:

```xml

<bpmn:serviceTask id="task1" name="Java Delegate">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="camunda-7-adapter"/>
    <zeebe:taskHeaders>
      <zeebe:header key="class" value="org.camunda.community.migration.example.SampleJavaDelegate"/>
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

```xml

<bpmn:serviceTask id="task2" name="Delegate Expression">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="camunda-7-adapter"/>
    <zeebe:taskHeaders>
      <zeebe:header key="delegateExpression" value="${myAwesomeJavaDelegateBean}"/>
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

If you are using an expression, you might want to write the result to a process variable. This can be achieved by setting an additional `resultVariable` header:

```xml

<bpmn:serviceTask id="task3" name="Expression">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="camunda-7-adapter"/>
    <zeebe:taskHeaders>
      <zeebe:header key="expression" value="${someBean.awesomeMethod(execution, someVar)}"/>
      <zeebe:header key="resultVariable" value="awesomeMethodResult"/>
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

The external task workers can be mapped by using the `taskType` and insert the
`topicName` there.

```xml

<bpmn:serviceTask id="task4" name="External Task Worker">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="my-awesome-topic"/>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

## Using Execution listener

To use execution listeners, use the header `executionListener.start` or `executionListener.end`. The value should be of type `delegateExpression` which returns a bean of type `ExecutionListener`:

```xml

<bpmn:serviceTask id="task3" name="Expression">
  <bpmn:extensionElements>
    <zeebe:taskHeaders>
      <zeebe:header key="executionListener.start" value="${startExecutionListenerBean}"/>
      <zeebe:header key="executionListener.end" value="${endExecutionListenerBean}"/>
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

## Example

Check out
[the full example](../example/process-solution-migrated/src/main/resources/process.bpmn)
for more details.
