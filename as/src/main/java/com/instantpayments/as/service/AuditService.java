package com.instantpayments.as.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.instantpayments.as.entity.AuditLog;
import com.instantpayments.as.repository.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogRepository auditLogRepository;
  private final CdcEventParser cdcEventParser;

  public AuditService(AuditLogRepository auditLogRepository, CdcEventParser cdcEventParser) {
    this.auditLogRepository = auditLogRepository;
    this.cdcEventParser = cdcEventParser;
  }

  @Transactional
  public AuditLog ingestCdcEvent(JsonNode event) {
    AuditLog auditLog = cdcEventParser.parse(event);
    auditLog = auditLogRepository.save(auditLog);
    log.info(
        "Persisted audit log #{} for transaction {} ({})",
        auditLog.getId(),
        auditLog.getTransactionId(),
        auditLog.getOperation());
    return auditLog;
  }

  public List<AuditLog> getChangeHistory(UUID transactionId) {
    return auditLogRepository.findByTransactionIdOrderBySourceTsAsc(transactionId);
  }

  public Page<AuditLog> findAll(Pageable pageable) {
    return auditLogRepository.findAll(pageable);
  }

  public Page<AuditLog> findByTransactionId(UUID transactionId, Pageable pageable) {
    return auditLogRepository.findByTransactionId(transactionId, pageable);
  }

  public Page<AuditLog> findByOperation(String operation, Pageable pageable) {
    return auditLogRepository.findByOperation(operation, pageable);
  }
}
