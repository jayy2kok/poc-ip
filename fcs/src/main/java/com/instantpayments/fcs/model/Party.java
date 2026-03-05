package com.instantpayments.fcs.model;

import jakarta.xml.bind.annotation.*;

/**
 * Party (payer/payee) within a fraud check request.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Party {

    @XmlElement(namespace = "http://poc.instantpayments/fraud")
    private String name;

    @XmlElement(namespace = "http://poc.instantpayments/fraud")
    private String bank;

    @XmlElement(namespace = "http://poc.instantpayments/fraud")
    private String countryCode;

    @XmlElement(namespace = "http://poc.instantpayments/fraud")
    private String account;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
}
