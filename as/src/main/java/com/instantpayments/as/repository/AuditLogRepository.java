package com.instantpayments.as.repository;

import com.instantpayments.as.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTransactionIdOrderBySourceTsAsc(UUID transactionId);

    Page<AuditLog> findByTransactionId(UUID transactionId, Pageable pageable);

    Page<AuditLog> findByOperation(String operation, Pageable pageable);
}
