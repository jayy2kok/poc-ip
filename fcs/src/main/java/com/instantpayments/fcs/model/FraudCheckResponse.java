package com.instantpayments.fcs.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlTransient;
import java.time.Instant;

/** JAXB model for fraud check response XML. */
@XmlRootElement(name = "FraudCheckResponse", namespace = "http://poc.instantpayments/fraud")
@XmlAccessorType(XmlAccessType.FIELD)
public class FraudCheckResponse {

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String transactionId;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String outcome;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String message;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  @XmlSchemaType(name = "dateTime")
  private String checkedAt;

  @XmlTransient private Instant checkedAtInstant;

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
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

  public String getCheckedAt() {
    return checkedAt;
  }

  public void setCheckedAt(Instant instant) {
    this.checkedAtInstant = instant;
    this.checkedAt = instant.toString();
  }
}
