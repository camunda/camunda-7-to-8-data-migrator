package io.camunda.migrator;

import io.camunda.zeebe.protocol.Protocol;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class ConverterUtil {

  public static Long convertJobDefinitionIdToKey(String jobDefinitionId) {
    return convertIdToKey(jobDefinitionId); //TODO check correctness
  }

  public static Long convertProcessDefinitionIdToKey(String processDefinitionId) {
    // The process definition id consists of <proc def key>:<version>:<id>
    // Split it up and only pass the id
    return convertIdToKey(processDefinitionId.split(":")[2]);
  }

  public static Long convertActivityInstanceIdToKey(String id) {
    if (isUserTak(id)) {
      // User tasks have no composite keys
      return convertIdToKey(id);
    } else {
      // All other flow nodes have composite keys
      return convertIdToKey(id.split(":")[1]);
    }
  }

  private static boolean isUserTak(String id) {
    return !id.contains(":");
  }

  public static Long convertIdToKey(String id) {
    // The C7 ID is UUID whereas C8 IDs are called keys.
    // C8 keys are a composite of the partition and the id.
    // TODO: convert C7 IDs correctly to C8 IDs.
    if (id == null) return null;
    return Protocol.encodePartitionId(99, Long.parseLong(id));
  }

  public static OffsetDateTime convertDate(Date date) {
    if (date == null) return null;
    return date.toInstant().atOffset(ZoneOffset.UTC);
  }

}
