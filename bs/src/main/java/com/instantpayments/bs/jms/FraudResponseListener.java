package com.instantpayments.bs.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.bs.converter.XmlJsonConverter;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens on fraud.response.queue for XML responses from FCS, converts XML→JSON, and sends
 * notification to payment.notification.queue.
 */
@Component
public class FraudResponseListener {

  private static final Logger log = LoggerFactory.getLogger(FraudResponseListener.class);

  private final XmlJsonConverter xmlJsonConverter;
  private final JmsTemplate jmsTemplate;
  private final ObjectMapper objectMapper;

  public FraudResponseListener(
      XmlJsonConverter xmlJsonConverter, JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
    this.xmlJsonConverter = xmlJsonConverter;
    this.jmsTemplate = jmsTemplate;
    this.objectMapper = objectMapper;
  }

  @JmsListener(destination = QueueNames.FRAUD_RESPONSE_QUEUE)
  public void onFraudResponse(String xmlMessage) {
    try {
      log.info("Received fraud response on {}", QueueNames.FRAUD_RESPONSE_QUEUE);

      PaymentNotification notification = xmlJsonConverter.fromFraudResponseXml(xmlMessage);
      String json = objectMapper.writeValueAsString(notification);

      jmsTemplate.convertAndSend(QueueNames.PAYMENT_NOTIFICATION_QUEUE, json);
      log.info(
          "Sent payment notification for transaction {} to {}: {}",
          notification.getTransactionId(),
          QueueNames.PAYMENT_NOTIFICATION_QUEUE,
          notification.getOutcome());

    } catch (Exception e) {
      log.error("Error processing fraud response", e);
    }
  }
}
