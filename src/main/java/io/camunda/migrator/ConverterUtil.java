package io.camunda.migrator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class ConverterUtil {

  public static String convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return processDefinitionId.split(":")[2];
  }

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

  public static OffsetDateTime convertDate(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC);
  }

}
