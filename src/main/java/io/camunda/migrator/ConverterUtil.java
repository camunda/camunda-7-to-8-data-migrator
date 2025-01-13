package io.camunda.migrator;

public class ConverterUtil {

  public static Long convertIdToKey(String id) {
    // The C7 ID is UUID whereas C8 IDs are called keys.
    // C8 keys are a composite of the partition and the id.
    // TODO: convert C7 IDs correctly to C8 IDs.
    if (id == null) {
      return null;
    }

    return Long.valueOf(id);
  }

  public static String convertActivityInstanceIdToKey(String id) {
    return id.split(":")[1];
  }

}
