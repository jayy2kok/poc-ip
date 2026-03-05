package com.instantpayments.bs.converter;

import com.instantpayments.common.dto.PaymentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Converts PaymentPayload JSON objects to XML fraud check request strings and fraud check response
 * XML strings back to JSON notifications.
 */
@Component
public class JsonXmlConverter {

  private static final Logger log = LoggerFactory.getLogger(JsonXmlConverter.class);
  private static final String NS = "http://poc.instantpayments/fraud";

  /** Convert PaymentPayload to FraudCheckRequest XML string. */
  public String toFraudCheckXml(PaymentPayload payload) {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<FraudCheckRequest xmlns=\"").append(NS).append("\">\n");
    xml.append("    <transactionId>")
        .append(payload.getTransactionId())
        .append("</transactionId>\n");
    xml.append("    <payer>\n");
    xml.append("        <name>").append(escapeXml(payload.getPayerName())).append("</name>\n");
    xml.append("        <bank>").append(escapeXml(payload.getPayerBank())).append("</bank>\n");
    xml.append("        <countryCode>")
        .append(payload.getPayerCountryCode())
        .append("</countryCode>\n");
    xml.append("        <account>").append(payload.getPayerAccount()).append("</account>\n");
    xml.append("    </payer>\n");
    xml.append("    <payee>\n");
    xml.append("        <name>").append(escapeXml(payload.getPayeeName())).append("</name>\n");
    xml.append("        <bank>").append(escapeXml(payload.getPayeeBank())).append("</bank>\n");
    xml.append("        <countryCode>")
        .append(payload.getPayeeCountryCode())
        .append("</countryCode>\n");
    xml.append("        <account>").append(payload.getPayeeAccount()).append("</account>\n");
    xml.append("    </payee>\n");
    if (payload.getPaymentInstruction() != null) {
      xml.append("    <paymentInstruction>")
          .append(escapeXml(payload.getPaymentInstruction()))
          .append("</paymentInstruction>\n");
    }
    xml.append("    <executionDate>")
        .append(payload.getExecutionDate())
        .append("</executionDate>\n");
    xml.append("    <amount>").append(payload.getAmount()).append("</amount>\n");
    xml.append("    <currency>").append(payload.getCurrency()).append("</currency>\n");
    xml.append("</FraudCheckRequest>");

    log.debug("Converted payment {} to XML", payload.getTransactionId());
    return xml.toString();
  }

  private String escapeXml(String value) {
    if (value == null) return "";
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
