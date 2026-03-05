package com.instantpayments.pps.repository;

import com.instantpayments.common.dto.PaymentStatus;
import com.instantpayments.pps.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionId(UUID transactionId);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
