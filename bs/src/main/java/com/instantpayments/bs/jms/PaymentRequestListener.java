package com.instantpayments.bs.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.bs.converter.JsonXmlConverter;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Solution 1: JMS-based inbound from PPS. Listens on payment.request.queue, converts JSON→XML,
 * sends to fraud.request.queue.
 */
@Component
@Profile("sol1")
public class PaymentRequestListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentRequestListener.class);

  private final JsonXmlConverter jsonXmlConverter;
  private final JmsTemplate jmsTemplate;
  private final ObjectMapper objectMapper;

  public PaymentRequestListener(
      JsonXmlConverter jsonXmlConverter, JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
    this.jsonXmlConverter = jsonXmlConverter;
    this.jmsTemplate = jmsTemplate;
    this.objectMapper = objectMapper;
  }

  @JmsListener(destination = QueueNames.PAYMENT_REQUEST_QUEUE)
  public void onPaymentRequest(String jsonMessage) {
    try {
      log.info("Received payment request on {}", QueueNames.PAYMENT_REQUEST_QUEUE);

      PaymentPayload payload = objectMapper.readValue(jsonMessage, PaymentPayload.class);
      String fraudCheckXml = jsonXmlConverter.toFraudCheckXml(payload);

      jmsTemplate.convertAndSend(QueueNames.FRAUD_REQUEST_QUEUE, fraudCheckXml);
      log.info(
          "Forwarded fraud check request for transaction {} to {}",
          payload.getTransactionId(),
          QueueNames.FRAUD_REQUEST_QUEUE);

    } catch (Exception e) {
      log.error("Error processing payment request", e);
    }
  }
}
