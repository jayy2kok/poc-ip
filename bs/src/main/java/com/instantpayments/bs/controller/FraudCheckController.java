package com.instantpayments.bs.controller;

import com.instantpayments.bs.api.FraudCheckApi;
import com.instantpayments.bs.api.model.FraudCheckAcceptedResponse;
import com.instantpayments.bs.api.model.FraudCheckRequest;
import com.instantpayments.bs.converter.JsonXmlConverter;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Solution 2: REST-based inbound from PPS.
 * Receives JSON payment payload via REST, converts to XML, sends to FCS via
 * JMS.
 */
@RestController
@Profile("sol2")
public class FraudCheckController implements FraudCheckApi {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckController.class);

    private final JsonXmlConverter jsonXmlConverter;
    private final JmsTemplate jmsTemplate;

    public FraudCheckController(JsonXmlConverter jsonXmlConverter, JmsTemplate jmsTemplate) {
        this.jsonXmlConverter = jsonXmlConverter;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public ResponseEntity<FraudCheckAcceptedResponse> submitFraudCheck(FraudCheckRequest fraudCheckRequest) {
        log.info("Received fraud check request via REST for transaction {}", fraudCheckRequest.getTransactionId());

        // Map generated DTO → internal DTO
        PaymentPayload payload = toPayload(fraudCheckRequest);
        String fraudCheckXml = jsonXmlConverter.toFraudCheckXml(payload);

        jmsTemplate.convertAndSend(QueueNames.FRAUD_REQUEST_QUEUE, fraudCheckXml);
        log.info("Forwarded fraud check to FCS for transaction {}", fraudCheckRequest.getTransactionId());

        FraudCheckAcceptedResponse response = new FraudCheckAcceptedResponse();
        response.setTransactionId(UUID.fromString(fraudCheckRequest.getTransactionId().toString()));
        response.setStatus(FraudCheckAcceptedResponse.StatusEnum.PROCESSING);
        response.setMessage("Fraud check request accepted. Result will be delivered via notification queue.");

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
