package com.instantpayments.bs.converter;

import com.instantpayments.common.dto.PaymentNotification;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.UUID;

/**
 * Converts FraudCheckResponse XML to PaymentNotification JSON DTO.
 */
@Component
public class XmlJsonConverter {

    private static final Logger log = LoggerFactory.getLogger(XmlJsonConverter.class);

    /**
     * Parse fraud check response XML and convert to PaymentNotification.
     */
    public PaymentNotification fromFraudResponseXml(String xml) {
        try {
            // Simple XML parsing using StAX
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

            String transactionId = null;
            String outcome = null;
            String message = null;
            String checkedAt = null;
            String currentElement = null;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT) {
                    currentElement = reader.getLocalName();
                } else if (event == XMLStreamReader.CHARACTERS && currentElement != null) {
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        switch (currentElement) {
                            case "transactionId" -> transactionId = text;
                            case "outcome" -> outcome = text;
                            case "message" -> message = text;
                            case "checkedAt" -> checkedAt = text;
                        }
                    }
                }
            }

            PaymentNotification notification = new PaymentNotification(
                    UUID.fromString(transactionId),
                    outcome,
                    message,
                    checkedAt != null ? Instant.parse(checkedAt) : Instant.now()
            );

            log.debug("Converted XML response for transaction {} to JSON notification", transactionId);
            return notification;

        } catch (Exception e) {
            log.error("Error parsing fraud response XML", e);
            throw new RuntimeException("Failed to parse fraud response XML", e);
        }
    }
}
