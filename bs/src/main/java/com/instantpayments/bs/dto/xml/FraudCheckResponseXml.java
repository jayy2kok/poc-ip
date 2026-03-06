package com.instantpayments.bs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;

/**
 * JAXB model for the FraudCheckResponse XML received from FCS. Matches the FCS FraudCheckResponse
 * model / fraud-check namespace exactly.
 */
@XmlRootElement(name = "FraudCheckResponse", namespace = "http://poc.instantpayments/fraud")
@XmlAccessorType(XmlAccessType.FIELD)
public class FraudCheckResponseXml {

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String transactionId;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String outcome;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String message;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  @XmlSchemaType(name = "dateTime")
  private String checkedAt;

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

  public void setCheckedAt(String checkedAt) {
    this.checkedAt = checkedAt;
  }
}
