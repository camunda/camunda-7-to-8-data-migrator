package io.camunda.migrator;

import io.camunda.zeebe.protocol.Protocol;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class ConverterUtil {

  /* Create data on a partition that doesn't collide with Zeebe */
  public static int C7_HISTORY_PARTITION_ID = 99;

  public static Long getNextKey() {
    SecureRandom secureRandom = new SecureRandom();
    return Protocol.encodePartitionId(C7_HISTORY_PARTITION_ID, Math.abs(secureRandom.nextLong()));
  }

  public static OffsetDateTime convertDate(Date date) {
    if (date == null) return null;
    return date.toInstant().atOffset(ZoneOffset.UTC);
  }

}
