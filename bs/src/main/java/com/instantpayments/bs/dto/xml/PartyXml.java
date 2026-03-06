package com.instantpayments.bs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** Party (payer/payee) within a fraud check request. Matches FCS Party model. */
@XmlAccessorType(XmlAccessType.FIELD)
public class PartyXml {

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String name;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String bank;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String countryCode;

  @XmlElement(namespace = "http://poc.instantpayments/fraud")
  private String account;

  public PartyXml() {}

  public PartyXml(String name, String bank, String countryCode, String account) {
    this.name = name;
    this.bank = bank;
    this.countryCode = countryCode;
    this.account = account;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBank() {
    return bank;
  }

  public void setBank(String bank) {
    this.bank = bank;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }
}
