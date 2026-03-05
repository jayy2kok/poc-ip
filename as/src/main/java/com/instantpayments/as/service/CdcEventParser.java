package com.instantpayments.as.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.as.entity.AuditLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Parses Debezium CDC change events and creates AuditLog entities. */
@Component
public class CdcEventParser {

  private static final Logger log = LoggerFactory.getLogger(CdcEventParser.class);
  private static final Map<String, String> OP_MAP =
      Map.of(
          "c", "INSERT",
          "u", "UPDATE",
          "d", "DELETE",
          "r", "INSERT" // snapshot reads treated as inserts
          );

  private final ObjectMapper objectMapper;

  public CdcEventParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AuditLog parse(JsonNode event) {
    try {
      String op = event.path("op").asText();
      String operation = OP_MAP.getOrDefault(op, "UNKNOWN");

      long tsMs = event.path("ts_ms").asLong();
      Instant sourceTs = Instant.ofEpochMilli(tsMs);

      JsonNode before = event.path("before");
      JsonNode after = event.path("after");

      // Extract transaction_id from after (or before for delete)
      JsonNode stateNode = after.isMissingNode() || after.isNull() ? before : after;
      String transactionIdStr = stateNode.path("transaction_id").asText();
      UUID transactionId = UUID.fromString(transactionIdStr);

      AuditLog auditLog = new AuditLog();
      auditLog.setTransactionId(transactionId);
      auditLog.setOperation(operation);
      auditLog.setSourceTs(sourceTs);

      // Set before/after state as JSONB strings
      if (!before.isMissingNode() && !before.isNull()) {
        auditLog.setBeforeState(objectMapper.writeValueAsString(before));
      }
      if (!after.isMissingNode() && !after.isNull()) {
        auditLog.setAfterState(objectMapper.writeValueAsString(after));
      }

      // Calculate changed fields for UPDATEs
      if ("UPDATE".equals(operation)
          && !before.isMissingNode()
          && !before.isNull()
          && !after.isMissingNode()
          && !after.isNull()) {
        List<String> changed = new ArrayList<>();
        Iterator<String> fieldNames = after.fieldNames();
        while (fieldNames.hasNext()) {
          String field = fieldNames.next();
          JsonNode beforeVal = before.path(field);
          JsonNode afterVal = after.path(field);
          if (!beforeVal.equals(afterVal)) {
            changed.add(field);
          }
        }
        auditLog.setChangedFields(changed.toArray(new String[0]));
      }

      log.debug("Parsed CDC event: {} on transaction {}", operation, transactionId);
      return auditLog;

    } catch (Exception e) {
      log.error("Failed to parse CDC event", e);
      throw new RuntimeException("Failed to parse CDC event", e);
    }
  }
}
