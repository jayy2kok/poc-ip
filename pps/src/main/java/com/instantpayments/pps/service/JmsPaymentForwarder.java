package com.instantpayments.pps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Solution 1: Forward payment to BS via JMS (payment.request.queue).
 */
@Component
@Profile("sol1")
public class JmsPaymentForwarder implements PaymentForwarder {

    private static final Logger log = LoggerFactory.getLogger(JmsPaymentForwarder.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public JmsPaymentForwarder(JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(PaymentPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            jmsTemplate.convertAndSend(QueueNames.PAYMENT_REQUEST_QUEUE, json);
            log.info("Forwarded payment {} to {} via JMS", payload.getTransactionId(), QueueNames.PAYMENT_REQUEST_QUEUE);
        } catch (Exception e) {
            log.error("Failed to forward payment {} via JMS", payload.getTransactionId(), e);
            throw new RuntimeException("Failed to forward payment", e);
        }
    }
}
