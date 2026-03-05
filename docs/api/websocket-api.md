# WebSocket API — Payment Notifications

## Overview

The PPS exposes a WebSocket endpoint using **STOMP over WebSocket** for real-time payment status updates. Clients subscribe to receive notifications when a payment transitions from `PENDING` to `APPROVED` or `REJECTED`.

---

## Connection Details

| Property | Value |
|---|---|
| WebSocket URL | `ws://localhost:8081/ws/payments` |
| Protocol | STOMP over WebSocket |
| SockJS Fallback | `http://localhost:8081/ws/payments` |

---

## Subscribe Destinations

### All Payment Updates
```
/topic/payments
```
Receives all payment status change notifications.

### Specific Transaction Updates
```
/topic/payments/{transactionId}
```
Receives updates for a specific transaction only.

---

## Message Format

### Payment Status Notification (Server → Client)

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "APPROVED",
  "previousStatus": "PENDING",
  "updatedAt": "2026-03-04T12:30:05.000Z"
}
```

> **Note:** Fraud check details (e.g., rejection reasons) are **internal** and are not exposed to clients. Only the payment status is communicated via WebSocket.

### Fields

| Field | Type | Description |
|---|---|---|
| `transactionId` | UUID | Unique transaction identifier |
| `status` | String | New status: `APPROVED` or `REJECTED` |
| `previousStatus` | String | Previous status (always `PENDING`) |
| `updatedAt` | ISO 8601 | Timestamp of status change |

---

## Client Integration (JavaScript)

```javascript
const socket = new SockJS('/ws/payments');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to all payment updates
    stompClient.subscribe('/topic/payments', function(message) {
        const notification = JSON.parse(message.body);
        console.log('Payment update:', notification);
        // Update UI with notification.status
    });

    // Or subscribe to a specific transaction
    stompClient.subscribe('/topic/payments/550e8400-e29b-41d4-a716-446655440000', 
        function(message) {
            const notification = JSON.parse(message.body);
            console.log('Transaction update:', notification);
        }
    );
});
```

---

## Audit Trail

Payment changes are captured automatically via **Debezium CDC** (Change Data Capture) on the `payments` table and persisted by the **Audit Service**. Audit logs are not part of the WebSocket API — they are available via the Audit Service REST API (`GET /api/v1/audit-logs`). See the [Audit Service API spec](as-api.yaml) for details.
