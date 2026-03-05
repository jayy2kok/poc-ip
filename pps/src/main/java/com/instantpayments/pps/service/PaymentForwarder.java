package com.instantpayments.pps.service;

import com.instantpayments.common.dto.PaymentPayload;

/**
 * Strategy interface for forwarding payments.
 * Implemented by Sol1 (JMS) and Sol2 (REST) forwarders.
 */
public interface PaymentForwarder {

    void forward(PaymentPayload payload);
}
