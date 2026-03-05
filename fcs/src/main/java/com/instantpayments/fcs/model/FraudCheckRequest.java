package com.instantpayments.fcs.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** JAXB model for fraud check request XML. Matches the fraud-check.xsd FraudCheckRequestType. */
@XmlRootElement(name = "FraudCheckRequest", namespace = "http://poc.instantpayments/fraud")
@XmlAccessorType(XmlAccessType.FIELD)
public class FraudCheckRequest {

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String transactionId;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private Party payer;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private Party payee;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String paymentInstruction;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String executionDate;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String amount;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String currency;

  // Getters & Setters
  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Party getPayer() {
    return payer;
  }

  public void setPayer(Party payer) {
    this.payer = payer;
  }

  public Party getPayee() {
    return payee;
  }

  public void setPayee(Party payee) {
    this.payee = payee;
  }

  public String getPaymentInstruction() {
    return paymentInstruction;
  }

  public void setPaymentInstruction(String paymentInstruction) {
    this.paymentInstruction = paymentInstruction;
  }

  public String getExecutionDate() {
    return executionDate;
  }

  public void setExecutionDate(String executionDate) {
    this.executionDate = executionDate;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }
}
