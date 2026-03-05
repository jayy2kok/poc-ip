package com.instantpayments.common.dto;

import java.time.Instant;
import java.util.UUID;

/** Notification sent from BS to PPS with the fraud check result. */
public class PaymentNotification {

  private UUID transactionId;
  private String outcome; // "APPROVED" or "REJECTED"
  private String message;
  private Instant checkedAt;

  public PaymentNotification() {}

  public PaymentNotification(
      UUID transactionId, String outcome, String message, Instant checkedAt) {
    this.transactionId = transactionId;
    this.outcome = outcome;
    this.message = message;
    this.checkedAt = checkedAt;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(UUID transactionId) {
    this.transactionId = transactionId;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Instant getCheckedAt() {
    return checkedAt;
  }

  public void setCheckedAt(Instant checkedAt) {
    this.checkedAt = checkedAt;
  }
}
