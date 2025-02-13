package io.camunda.migrator.testdata;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.camunda.bpm.engine.variable.Variables.objectValue;

@Component
public class AddVariablesService implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariables(createVariables());
  }
  protected VariableMap createVariables() {

    List<String> serializable = new ArrayList<String>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");
    byte[] bytes = "somebytes".getBytes();

    CockpitVariable cockpitVar = new CockpitVariable("test", "cockpitVariableValue");
    cockpitVar.getDates().add(new Date());

    // set JSON variable
    JsonSerialized jsonSerialized = new JsonSerialized();
    jsonSerialized.setFoo("bar");


    // set JAXB variable
    JaxBSerialized jaxBSerialized = new JaxBSerialized();
    jaxBSerialized.setFoo("bar");

    return Variables
      .putValue("shortVar", (short) 123)
      .putValue("longVar", 928374L)
      .putValue("integerVar", 1234)
      .putValue("floatVar", Float.MAX_VALUE)
      .putValue("doubleVar", Double.MAX_VALUE)
      .putValue("trueBooleanVar", true)
      .putValue("falseBooleanVar", false)
      .putValue("stringVar", "coca-cola")
      .putValue("dateVar", new Date())
      .putValue("nullVar", null)
      .putValue("serializableVar", serializable)
      .putValue("bytesVar", bytes)
      .putValue("aByteVar", Byte.parseByte("1", 2)) // 2 for binary
      .putValue("value1", "xyz")
      .putValue("random", (int)(Math.random() * 100))
      .putValue("cockpitVar", cockpitVar)
      .putValue("xmlSerializable", objectValue(jaxBSerialized).serializationDataFormat("application/xml"))
      .putValue("jsonSerializable", objectValue(jsonSerialized).serializationDataFormat("application/json"));

  }

}
