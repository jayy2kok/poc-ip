package com.instantpayments.pps.controller;

import com.instantpayments.common.dto.PaymentPayload;
import com.instantpayments.pps.api.PaymentsApi;
import com.instantpayments.pps.api.model.PagedPaymentResponse;
import com.instantpayments.pps.api.model.PaymentDetailResponse;
import com.instantpayments.pps.api.model.PaymentRequest;
import com.instantpayments.pps.api.model.PaymentResponse;
import com.instantpayments.pps.api.model.PaymentStatus;
import com.instantpayments.pps.api.model.PaymentStatusResponse;
import com.instantpayments.pps.entity.Payment;
import com.instantpayments.pps.service.PaymentForwarder;
import com.instantpayments.pps.service.PaymentService;
import com.instantpayments.pps.service.PaymentValidator;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class PaymentController implements PaymentsApi {

  private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

  private final PaymentService paymentService;
  private final PaymentValidator paymentValidator;
  private final PaymentForwarder paymentForwarder;

  public PaymentController(
      PaymentService paymentService,
      PaymentValidator paymentValidator,
      PaymentForwarder paymentForwarder) {
    this.paymentService = paymentService;
    this.paymentValidator = paymentValidator;
    this.paymentForwarder = paymentForwarder;
  }

  @Override
  public ResponseEntity<PaymentResponse> submitPayment(PaymentRequest paymentRequest) {
    // Map generated DTO → internal DTO
    PaymentPayload payload = toPayload(paymentRequest);

    // Validate
    List<Map<String, String>> errors = paymentValidator.validate(payload);
    if (!errors.isEmpty()) {
      // Return 400 — we can't return ValidationErrorResponse from a PaymentResponse
      // method,
      // so we throw to let the global handler deal with it, or return raw
      log.warn("Validation failed with {} errors", errors.size());
      return ResponseEntity.badRequest().build();
    }

    // Create payment
    Payment payment = paymentService.createPayment(payload);

    // Forward for fraud check
    PaymentPayload forwardPayload = paymentService.toPayload(payment);
    paymentForwarder.forward(forwardPayload);

    // Build response
    PaymentResponse response = new PaymentResponse();
    response.setTransactionId(payment.getTransactionId());
    response.setStatus(PaymentStatus.PENDING);
    response.setMessage("Payment accepted for processing");
    response.setCreationTimestamp(payment.getCreatedAt().atOffset(ZoneOffset.UTC));

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  @Override
  public ResponseEntity<PagedPaymentResponse> listPayments(
      PaymentStatus status, Integer page, Integer size, String sort) {
    String[] sortParts = sort.split(",");
    Sort.Direction direction =
        sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

    com.instantpayments.common.dto.PaymentStatus commonStatus =
        status != null ? com.instantpayments.common.dto.PaymentStatus.valueOf(status.name()) : null;

    Page<Payment> payments =
        commonStatus != null
            ? paymentService.findByStatus(commonStatus, pageable)
            : paymentService.findAll(pageable);

    PagedPaymentResponse response = new PagedPaymentResponse();
    response.setContent(payments.getContent().stream().map(this::toDetailResponse).toList());
    response.setPage(payments.getNumber());
    response.setSize(payments.getSize());
    response.setTotalElements(payments.getTotalElements());
    response.setTotalPages(payments.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<PaymentDetailResponse> getPaymentByTransactionId(UUID transactionId) {
    return paymentService
        .findByTransactionId(transactionId)
        .map(p -> ResponseEntity.ok(toDetailResponse(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getPaymentStatus(UUID transactionId) {
    return paymentService
        .findByTransactionId(transactionId)
        .map(
            p -> {
              PaymentStatusResponse resp = new PaymentStatusResponse();
              resp.setTransactionId(p.getTransactionId());
              resp.setStatus(PaymentStatus.valueOf(p.getStatus().name()));
              resp.setUpdatedAt(p.getUpdatedAt().atOffset(ZoneOffset.UTC));
              return ResponseEntity.ok(resp);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  // --- Mapping helpers ---

  private PaymentPayload toPayload(PaymentRequest req) {
    PaymentPayload p = new PaymentPayload();
    p.setPayerName(req.getPayerName());
    p.setPayerBank(req.getPayerBank());
    p.setPayerCountryCode(req.getPayerCountryCode());
    p.setPayerAccount(req.getPayerAccount());
    p.setPayeeName(req.getPayeeName());
    p.setPayeeBank(req.getPayeeBank());
    p.setPayeeCountryCode(req.getPayeeCountryCode());
    p.setPayeeAccount(req.getPayeeAccount());
    p.setPaymentInstruction(req.getPaymentInstruction());
    p.setExecutionDate(req.getExecutionDate());
    p.setAmount(req.getAmount() != null ? java.math.BigDecimal.valueOf(req.getAmount()) : null);
    p.setCurrency(req.getCurrency());
    return p;
  }

  private PaymentDetailResponse toDetailResponse(Payment p) {
    PaymentDetailResponse r = new PaymentDetailResponse();
    r.setTransactionId(p.getTransactionId());
    r.setPayerName(p.getPayerName());
    r.setPayerBank(p.getPayerBank());
    r.setPayerCountryCode(p.getPayerCountry());
    r.setPayerAccount(p.getPayerAccount());
    r.setPayeeName(p.getPayeeName());
    r.setPayeeBank(p.getPayeeBank());
    r.setPayeeCountryCode(p.getPayeeCountry());
    r.setPayeeAccount(p.getPayeeAccount());
    r.setPaymentInstruction(p.getPaymentInstruction());
    r.setExecutionDate(p.getExecutionDate());
    r.setAmount(p.getAmount().doubleValue());
    r.setCurrency(p.getCurrency());
    r.setStatus(PaymentStatus.valueOf(p.getStatus().name()));
    r.setCreatedAt(p.getCreatedAt().atOffset(ZoneOffset.UTC));
    r.setUpdatedAt(p.getUpdatedAt().atOffset(ZoneOffset.UTC));
    return r;
  }
}
