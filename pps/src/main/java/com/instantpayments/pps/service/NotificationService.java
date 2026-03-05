package com.instantpayments.pps.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Sends real-time payment status notifications via WebSocket (STOMP).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendStatusUpdate(UUID transactionId, String newStatus, String previousStatus) {
        Map<String, Object> notification = Map.of(
                "transactionId", transactionId.toString(),
                "status", newStatus,
                "previousStatus", previousStatus,
                "updatedAt", Instant.now().toString()
        );

        // Send to topic for all subscribers
        messagingTemplate.convertAndSend("/topic/payments", notification);

        // Send to transaction-specific topic
        messagingTemplate.convertAndSend("/topic/payments/" + transactionId, notification);

        log.info("Sent WebSocket notification for transaction {}: {} → {}",
                transactionId, previousStatus, newStatus);
    }
}
