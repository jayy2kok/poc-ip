package com.instantpayments.pps.service;

import com.instantpayments.common.dto.PaymentPayload;
import com.instantpayments.common.dto.PaymentStatus;
import com.instantpayments.pps.entity.Payment;
import com.instantpayments.pps.repository.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;

  public PaymentService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Transactional
  public Payment createPayment(PaymentPayload payload) {
    Payment payment = new Payment();
    payment.setTransactionId(UUID.randomUUID());
    payment.setPayerName(payload.getPayerName());
    payment.setPayerBank(payload.getPayerBank());
    payment.setPayerCountry(payload.getPayerCountryCode());
    payment.setPayerAccount(payload.getPayerAccount());
    payment.setPayeeName(payload.getPayeeName());
    payment.setPayeeBank(payload.getPayeeBank());
    payment.setPayeeCountry(payload.getPayeeCountryCode());
    payment.setPayeeAccount(payload.getPayeeAccount());
    payment.setPaymentInstruction(payload.getPaymentInstruction());
    payment.setExecutionDate(payload.getExecutionDate());
    payment.setAmount(payload.getAmount());
    payment.setCurrency(payload.getCurrency());
    payment.setStatus(PaymentStatus.PENDING);

    payment = paymentRepository.save(payment);
    log.info("Created payment with transactionId: {}", payment.getTransactionId());
    return payment;
  }

  @Transactional
  public Payment updateStatus(UUID transactionId, PaymentStatus status, String fraudMessage) {
    Payment payment =
        paymentRepository
            .findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionId));

    PaymentStatus previousStatus = payment.getStatus();
    payment.setStatus(status);
    payment.setFraudMessage(fraudMessage);
    payment = paymentRepository.save(payment);

    log.info("Updated payment {} status: {} → {}", transactionId, previousStatus, status);
    return payment;
  }

  public Optional<Payment> findByTransactionId(UUID transactionId) {
    return paymentRepository.findByTransactionId(transactionId);
  }

  public Page<Payment> findAll(Pageable pageable) {
    return paymentRepository.findAll(pageable);
  }

  public Page<Payment> findByStatus(PaymentStatus status, Pageable pageable) {
    return paymentRepository.findByStatus(status, pageable);
  }

  /** Convert a Payment entity to a PaymentPayload DTO for JMS transmission. */
  public PaymentPayload toPayload(Payment payment) {
    PaymentPayload payload = new PaymentPayload();
    payload.setTransactionId(payment.getTransactionId());
    payload.setPayerName(payment.getPayerName());
    payload.setPayerBank(payment.getPayerBank());
    payload.setPayerCountryCode(payment.getPayerCountry());
    payload.setPayerAccount(payment.getPayerAccount());
    payload.setPayeeName(payment.getPayeeName());
    payload.setPayeeBank(payment.getPayeeBank());
    payload.setPayeeCountryCode(payment.getPayeeCountry());
    payload.setPayeeAccount(payment.getPayeeAccount());
    payload.setPaymentInstruction(payment.getPaymentInstruction());
    payload.setExecutionDate(payment.getExecutionDate());
    payload.setAmount(payment.getAmount());
    payload.setCurrency(payment.getCurrency());
    payload.setCreationTimestamp(payment.getCreatedAt());
    return payload;
  }
}
