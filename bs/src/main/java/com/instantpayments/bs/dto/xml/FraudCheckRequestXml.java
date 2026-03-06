package com.instantpayments.bs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * JAXB model for the FraudCheckRequest XML sent from BS to FCS. Matches the FCS FraudCheckRequest
 * model / fraud-check namespace exactly.
 */
@XmlRootElement(name = "FraudCheckRequest", namespace = "http://poc.instantpayments/fraud")
@XmlAccessorType(XmlAccessType.FIELD)
public class FraudCheckRequestXml {

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String transactionId;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private PartyXml payer;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private PartyXml payee;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String paymentInstruction;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String executionDate;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String amount;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String currency;

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public PartyXml getPayer() {
    return payer;
  }

  public void setPayer(PartyXml payer) {
    this.payer = payer;
  }

  public PartyXml getPayee() {
    return payee;
  }

  public void setPayee(PartyXml payee) {
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
