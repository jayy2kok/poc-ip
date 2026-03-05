package com.instantpayments.pps.service;

import com.instantpayments.common.dto.PaymentPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates payment request fields: - Required fields not blank - ISO 3166-1 alpha-3 country codes
 * - ISO 4217 currency codes - Amount > 0
 */
@Component
public class PaymentValidator {

  private static final Logger log = LoggerFactory.getLogger(PaymentValidator.class);

  // Common ISO 3166-1 alpha-3 country codes
  private static final Set<String> VALID_COUNTRIES =
      Set.of(
          "USA", "GBR", "DEU", "FRA", "JPN", "CHN", "IND", "BRA", "AUS", "CAN", "ITA", "ESP", "MEX",
          "KOR", "RUS", "NLD", "CHE", "SGP", "HKG", "NZL", "SWE", "NOR", "DNK", "FIN", "AUT", "BEL",
          "IRL", "PRT", "GRC", "POL", "CZE", "HUN", "ROU", "BGR", "HRV", "SVK", "SVN", "LTU", "LVA",
          "EST", "ZAF", "NGA", "KEN", "EGY", "MAR", "TUN", "GHA", "TZA", "UGA", "ETH", "SAU", "ARE",
          "QAT", "KWT", "BHR", "OMN", "JOR", "LBN", "ISR", "TUR", "ARG", "CHL", "COL", "PER", "VEN",
          "URY", "ECU", "BOL", "PRY", "GUY", "THA", "VNM", "MYS", "IDN", "PHL", "PAK", "BGD", "LKA",
          "MMR", "KHM", "CUB", "IRQ", "IRN", "PRK", "SDN",
          "SYR", // Blacklisted countries (but valid codes)
          "TWN", "ISL");

  // Common ISO 4217 currency codes
  private static final Set<String> VALID_CURRENCIES =
      Set.of(
          "USD", "EUR", "GBP", "JPY", "CNY", "INR", "BRL", "AUD", "CAD", "CHF", "SEK", "NOK", "DKK",
          "SGD", "HKD", "NZD", "KRW", "TRY", "ZAR", "MXN", "PLN", "CZK", "HUF", "RON", "BGN", "HRK",
          "RUB", "THB", "MYR", "IDR", "PHP", "VND", "SAR", "AED", "QAR", "KWD", "BHD", "OMR", "JOD",
          "ILS", "EGP", "NGN", "KES", "GHS", "TZS", "PKR", "BDT", "LKR", "ARS", "CLP", "COP",
          "PEN");

  /**
   * @return list of validation errors, empty if valid.
   */
  public List<Map<String, String>> validate(PaymentPayload payload) {
    List<Map<String, String>> errors = new ArrayList<>();

    requireNotBlank(errors, "payerName", payload.getPayerName());
    requireNotBlank(errors, "payerBank", payload.getPayerBank());
    requireNotBlank(errors, "payerAccount", payload.getPayerAccount());
    requireNotBlank(errors, "payeeName", payload.getPayeeName());
    requireNotBlank(errors, "payeeBank", payload.getPayeeBank());
    requireNotBlank(errors, "payeeAccount", payload.getPayeeAccount());

    if (payload.getPayerCountryCode() == null
        || !VALID_COUNTRIES.contains(payload.getPayerCountryCode())) {
      errors.add(
          Map.of(
              "field", "payerCountryCode", "message", "Invalid ISO 3166-1 alpha-3 country code"));
    }
    if (payload.getPayeeCountryCode() == null
        || !VALID_COUNTRIES.contains(payload.getPayeeCountryCode())) {
      errors.add(
          Map.of(
              "field", "payeeCountryCode", "message", "Invalid ISO 3166-1 alpha-3 country code"));
    }
    if (payload.getCurrency() == null || !VALID_CURRENCIES.contains(payload.getCurrency())) {
      errors.add(Map.of("field", "currency", "message", "Invalid ISO 4217 currency code"));
    }
    if (payload.getExecutionDate() == null) {
      errors.add(Map.of("field", "executionDate", "message", "Execution date is required"));
    }
    if (payload.getAmount() == null || payload.getAmount().signum() <= 0) {
      errors.add(Map.of("field", "amount", "message", "Amount must be greater than 0"));
    }

    if (!errors.isEmpty()) {
      log.warn("Validation failed with {} errors", errors.size());
    }
    return errors;
  }

  private void requireNotBlank(List<Map<String, String>> errors, String field, String value) {
    if (value == null || value.isBlank()) {
      errors.add(Map.of("field", field, "message", field + " is required"));
    }
  }
}
