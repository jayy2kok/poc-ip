package com.instantpayments.as.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.as.api.AuditLogsApi;
import com.instantpayments.as.api.CdcIngestApi;
import com.instantpayments.as.api.model.AuditLogEntry;
import com.instantpayments.as.api.model.CdcAckResponse;
import com.instantpayments.as.api.model.CdcChangeEvent;
import com.instantpayments.as.api.model.DiffEntry;
import com.instantpayments.as.api.model.FieldChange;
import com.instantpayments.as.api.model.PagedAuditLogResponse;
import com.instantpayments.as.entity.AuditLog;
import com.instantpayments.as.service.AuditService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AuditController implements CdcIngestApi, AuditLogsApi {

  private static final Logger log = LoggerFactory.getLogger(AuditController.class);

  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public AuditController(AuditService auditService, ObjectMapper objectMapper) {
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  // Resolve diamond with both CdcIngestApi and AuditLogsApi defining getRequest()
  @Override
  public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
    return java.util.Optional.empty();
  }

  /**
   * Raw CDC ingestion endpoint consumed by Debezium Server HTTP sink. Accepts the Debezium envelope
   * JSON directly (op / before / after / ts_ms) without going through the generated DTO which would
   * mangle the field names.
   */
  @PostMapping("/api/v1/cdc/payments")
  public ResponseEntity<CdcAckResponse> ingestRawCdcEvent(@RequestBody JsonNode event) {
    try {
      log.debug("Received raw CDC event: {}", event);
      AuditLog auditLog = auditService.ingestCdcEvent(event);

      CdcAckResponse response = new CdcAckResponse();
      response.setStatus(CdcAckResponse.StatusEnum.ACCEPTED);
      response.setAuditLogId(auditLog.getId());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to ingest CDC event", e);
      CdcAckResponse response = new CdcAckResponse();
      response.setStatus(CdcAckResponse.StatusEnum.REJECTED);
      return ResponseEntity.badRequest().body(response);
    }
  }

  /** Satisfies the generated CdcIngestApi interface (Swagger docs only). */
  @Override
  public ResponseEntity<CdcAckResponse> ingestCdcEvent(CdcChangeEvent cdcChangeEvent) {
    JsonNode event = objectMapper.valueToTree(cdcChangeEvent);
    return ingestRawCdcEvent(event);
  }

  @Override
  public ResponseEntity<PagedAuditLogResponse> listAuditLogs(
      UUID transactionId,
      String operation,
      OffsetDateTime fromDate,
      OffsetDateTime toDate,
      Integer page,
      Integer size,
      String sort) {
    String[] sortParts = sort.split(",");
    Sort.Direction direction =
        sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

    Page<AuditLog> logs;
    if (transactionId != null) {
      logs = auditService.findByTransactionId(transactionId, pageable);
    } else if (operation != null) {
      logs = auditService.findByOperation(operation, pageable);
    } else {
      logs = auditService.findAll(pageable);
    }

    PagedAuditLogResponse response = new PagedAuditLogResponse();
    response.setContent(logs.getContent().stream().map(this::toEntryResponse).toList());
    response.setPage(logs.getNumber());
    response.setSize(logs.getSize());
    response.setTotalElements(logs.getTotalElements());
    response.setTotalPages(logs.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<AuditLogEntry>> getChangeHistory(UUID transactionId) {
    List<AuditLog> history = auditService.getChangeHistory(transactionId);
    if (history.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(history.stream().map(this::toEntryResponse).toList());
  }

  @Override
  public ResponseEntity<List<DiffEntry>> getTransactionDiff(UUID transactionId) {
    List<AuditLog> history = auditService.getChangeHistory(transactionId);
    if (history.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    List<DiffEntry> diffs = new ArrayList<>();
    for (AuditLog entry : history) {
      if ("UPDATE".equals(entry.getOperation()) && entry.getChangedFields() != null) {
        try {
          Map<String, Object> before = parseJsonToMap(entry.getBeforeState());
          Map<String, Object> after = parseJsonToMap(entry.getAfterState());

          DiffEntry diff = new DiffEntry();
          diff.setSourceTs(entry.getSourceTs().atOffset(ZoneOffset.UTC));
          diff.setOperation(DiffEntry.OperationEnum.UPDATE);

          List<FieldChange> changes = new ArrayList<>();
          for (String field : entry.getChangedFields()) {
            FieldChange change = new FieldChange();
            change.setField(field);
            change.setOldValue(
                before.containsKey(field) ? String.valueOf(before.get(field)) : null);
            change.setNewValue(after.containsKey(field) ? String.valueOf(after.get(field)) : null);
            changes.add(change);
          }
          diff.setChanges(changes);
          diffs.add(diff);
        } catch (Exception e) {
          log.warn("Failed to compute diff for audit log #{}", entry.getId(), e);
        }
      }
    }

    return ResponseEntity.ok(diffs);
  }

  // --- Mapping helpers ---

  private AuditLogEntry toEntryResponse(AuditLog entry) {
    AuditLogEntry r = new AuditLogEntry();
    r.setId(entry.getId());
    r.setTransactionId(entry.getTransactionId());
    r.setOperation(AuditLogEntry.OperationEnum.fromValue(entry.getOperation()));
    r.setBeforeState(parseJsonToMap(entry.getBeforeState()));
    r.setAfterState(parseJsonToMap(entry.getAfterState()));
    r.setChangedFields(
        entry.getChangedFields() != null ? Arrays.asList(entry.getChangedFields()) : null);
    r.setSourceTs(entry.getSourceTs().atOffset(ZoneOffset.UTC));
    r.setCapturedAt(entry.getCapturedAt().atOffset(ZoneOffset.UTC));
    return r;
  }

  private Map<String, Object> parseJsonToMap(String json) {
    if (json == null) return null;
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse JSON to map: {}", e.getMessage());
      return Map.of("raw", json);
    }
  }
}
