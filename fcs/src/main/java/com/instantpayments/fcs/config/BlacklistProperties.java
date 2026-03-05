package com.instantpayments.fcs.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Externalized blacklist configuration loaded from application.yml. */
@Component
@ConfigurationProperties(prefix = "blacklist")
public class BlacklistProperties {

  private List<String> names = new ArrayList<>();
  private List<String> countries = new ArrayList<>();
  private List<String> banks = new ArrayList<>();
  private List<String> paymentInstructions = new ArrayList<>();

  public List<String> getNames() {
    return names;
  }

  public void setNames(List<String> names) {
    this.names = names;
  }

  public List<String> getCountries() {
    return countries;
  }

  public void setCountries(List<String> countries) {
    this.countries = countries;
  }

  public List<String> getBanks() {
    return banks;
  }

  public void setBanks(List<String> banks) {
    this.banks = banks;
  }

  public List<String> getPaymentInstructions() {
    return paymentInstructions;
  }

  public void setPaymentInstructions(List<String> paymentInstructions) {
    this.paymentInstructions = paymentInstructions;
  }
}
