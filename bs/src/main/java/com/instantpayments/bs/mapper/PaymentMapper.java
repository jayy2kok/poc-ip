package com.instantpayments.bs.mapper;

import com.instantpayments.bs.dto.xml.FraudCheckRequestXml;
import com.instantpayments.bs.dto.xml.FraudCheckResponseXml;
import com.instantpayments.bs.dto.xml.PartyXml;
import com.instantpayments.common.dto.PaymentNotification;
import com.instantpayments.common.dto.PaymentPayload;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps between internal DTOs and JAXB XML models. Used as a Camel bean inside Camel routes for pure
 * Java-to-Java transformation (no serialization logic).
 */
@Component("paymentMapper")
public class PaymentMapper {

  /** Convert a JSON PaymentPayload into a JAXB FraudCheckRequestXml. */
  public FraudCheckRequestXml toFraudCheckRequestXml(PaymentPayload payload) {
    FraudCheckRequestXml xml = new FraudCheckRequestXml();
    xml.setTransactionId(payload.getTransactionId().toString());

    PartyXml payer =
        new PartyXml(
            payload.getPayerName(),
            payload.getPayerBank(),
            payload.getPayerCountryCode(),
            payload.getPayerAccount());
    xml.setPayer(payer);

    PartyXml payee =
        new PartyXml(
            payload.getPayeeName(),
            payload.getPayeeBank(),
            payload.getPayeeCountryCode(),
            payload.getPayeeAccount());
    xml.setPayee(payee);

    xml.setPaymentInstruction(payload.getPaymentInstruction());
    xml.setExecutionDate(
        payload.getExecutionDate() != null ? payload.getExecutionDate().toString() : null);
    xml.setAmount(payload.getAmount() != null ? payload.getAmount().toPlainString() : null);
    xml.setCurrency(payload.getCurrency());

    return xml;
  }

  /** Convert a JAXB FraudCheckResponseXml into a JSON PaymentNotification. */
  public PaymentNotification toPaymentNotification(FraudCheckResponseXml response) {
    return new PaymentNotification(
        UUID.fromString(response.getTransactionId()),
        response.getOutcome(),
        response.getMessage(),
        response.getCheckedAt() != null ? Instant.parse(response.getCheckedAt()) : Instant.now());
  }
}
