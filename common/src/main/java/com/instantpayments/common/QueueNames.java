package com.instantpayments.common;

/** Shared JMS queue and topic names used across microservices. */
public final class QueueNames {

  private QueueNames() {}

  /** PPS → BS: payment for fraud check (Sol1) */
  public static final String PAYMENT_REQUEST_QUEUE = "payment.request.queue";

  /** BS → FCS: fraud check request (XML) */
  public static final String FRAUD_REQUEST_QUEUE = "fraud.request.queue";

  /** FCS → BS: fraud check response (XML) */
  public static final String FRAUD_RESPONSE_QUEUE = "fraud.response.queue";

  /** BS → PPS: fraud check notification (JSON) */
  public static final String PAYMENT_NOTIFICATION_QUEUE = "payment.notification.queue";
}
