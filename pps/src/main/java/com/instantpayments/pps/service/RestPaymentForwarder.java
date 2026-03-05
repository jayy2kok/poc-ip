package com.instantpayments.pps.service;

import com.instantpayments.common.dto.PaymentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Solution 2: Forward payment to BS via REST.
 */
@Component
@Profile("sol2")
public class RestPaymentForwarder implements PaymentForwarder {

    private static final Logger log = LoggerFactory.getLogger(RestPaymentForwarder.class);

    private final RestTemplate restTemplate;
    private final String bsUrl;

    public RestPaymentForwarder(RestTemplate restTemplate,
                                @Value("${pps.broker-system.url}") String bsUrl) {
        this.restTemplate = restTemplate;
        this.bsUrl = bsUrl;
    }

    @Override
    public void forward(PaymentPayload payload) {
        try {
            String url = bsUrl + "/api/v1/fraud-check";
            ResponseEntity<String> response = restTemplate.postForEntity(url, payload, String.class);
            log.info("Forwarded payment {} to BS via REST, response: {}",
                    payload.getTransactionId(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to forward payment {} via REST", payload.getTransactionId(), e);
            throw new RuntimeException("Failed to forward payment to BS", e);
        }
    }
}
