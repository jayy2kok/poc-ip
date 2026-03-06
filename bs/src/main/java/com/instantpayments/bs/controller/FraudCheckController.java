package com.instantpayments.bs.controller;

import com.instantpayments.bs.api.FraudCheckApi;
import com.instantpayments.bs.api.model.FraudCheckAcceptedResponse;
import com.instantpayments.bs.api.model.FraudCheckRequest;
import com.instantpayments.common.dto.PaymentPayload;
import java.util.UUID;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solution 2: REST-based inbound from PPS. Receives JSON payment payload via REST, delegates to
 * Camel route for JSON→XML conversion and JMS forwarding to FCS.
 */
@RestController
@Profile("sol2")
public class FraudCheckController implements FraudCheckApi {

  private static final Logger log = LoggerFactory.getLogger(FraudCheckController.class);

  private final ProducerTemplate producerTemplate;

  public FraudCheckController(ProducerTemplate producerTemplate) {
    this.producerTemplate = producerTemplate;
  }

  @Override
  public ResponseEntity<FraudCheckAcceptedResponse> submitFraudCheck(
      FraudCheckRequest fraudCheckRequest) {
    log.info(
        "Received fraud check request via REST for transaction {}",
        fraudCheckRequest.getTransactionId());

    // Map generated DTO → internal DTO
    PaymentPayload payload = toPayload(fraudCheckRequest);

    // Send to Camel route for JSON→XML conversion and JMS forwarding
    producerTemplate.sendBody("direct:fraudCheckRest", payload);
    log.info(
        "Forwarded fraud check to Camel route for transaction {}",
        fraudCheckRequest.getTransactionId());

    FraudCheckAcceptedResponse response = new FraudCheckAcceptedResponse();
    response.setTransactionId(UUID.fromString(fraudCheckRequest.getTransactionId().toString()));
    response.setStatus(FraudCheckAcceptedResponse.StatusEnum.PROCESSING);
    response.setMessage(
        "Fraud check request accepted. Result will be delivered via notification queue.");

    return ResponseEntity.accepted().body(response);
  }

  private PaymentPayload toPayload(FraudCheckRequest req) {
    PaymentPayload p = new PaymentPayload();
    p.setTransactionId(req.getTransactionId());
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
}
