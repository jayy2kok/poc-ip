package com.instantpayments.pps.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentNotification;
import com.instantpayments.common.dto.PaymentStatus;
import com.instantpayments.pps.entity.Payment;
import com.instantpayments.pps.service.NotificationService;
import com.instantpayments.pps.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Listens on payment.notification.queue for fraud check results from BS.
 * Updates payment status and sends WebSocket notification to UI.
 */
@Component
public class PaymentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationListener.class);

    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public PaymentNotificationListener(PaymentService paymentService,
                                       NotificationService notificationService,
                                       ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = QueueNames.PAYMENT_NOTIFICATION_QUEUE)
    public void onPaymentNotification(String jsonMessage) {
        try {
            log.info("Received payment notification on {}", QueueNames.PAYMENT_NOTIFICATION_QUEUE);

            PaymentNotification notification = objectMapper.readValue(jsonMessage, PaymentNotification.class);
            PaymentStatus newStatus = PaymentStatus.valueOf(notification.getOutcome());

            // Update payment status (fraud message stored internally, not exposed to client)
            Payment payment = paymentService.updateStatus(
                    notification.getTransactionId(),
                    newStatus,
                    notification.getMessage()
            );

            // Send WebSocket notification (status only, no fraud details)
            notificationService.sendStatusUpdate(
                    payment.getTransactionId(),
                    payment.getStatus().name(),
                    "PENDING"
            );

            log.info("Processed notification for transaction {}: {}",
                    notification.getTransactionId(), newStatus);

        } catch (Exception e) {
            log.error("Error processing payment notification", e);
        }
    }
}
