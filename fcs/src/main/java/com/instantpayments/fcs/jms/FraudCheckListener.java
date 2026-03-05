package com.instantpayments.fcs.jms;

import com.instantpayments.fcs.model.FraudCheckRequest;
import com.instantpayments.fcs.model.FraudCheckResponse;
import com.instantpayments.fcs.service.FraudCheckEngine;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens on fraud.request.queue for XML fraud check requests, processes them, and sends XML
 * responses to fraud.response.queue.
 */
@Component
public class FraudCheckListener {

  private static final Logger log = LoggerFactory.getLogger(FraudCheckListener.class);
  private static final String FRAUD_REQUEST_QUEUE = "fraud.request.queue";
  private static final String FRAUD_RESPONSE_QUEUE = "fraud.response.queue";

  private final FraudCheckEngine fraudCheckEngine;
  private final JmsTemplate jmsTemplate;
  private final JAXBContext jaxbContext;

  public FraudCheckListener(FraudCheckEngine fraudCheckEngine, JmsTemplate jmsTemplate)
      throws Exception {
    this.fraudCheckEngine = fraudCheckEngine;
    this.jmsTemplate = jmsTemplate;
    this.jaxbContext = JAXBContext.newInstance(FraudCheckRequest.class, FraudCheckResponse.class);
  }

  @JmsListener(destination = FRAUD_REQUEST_QUEUE)
  public void onFraudCheckRequest(String xmlMessage) {
    try {
      log.info("Received fraud check request on {}", FRAUD_REQUEST_QUEUE);

      // Unmarshal XML → FraudCheckRequest
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      FraudCheckRequest request =
          (FraudCheckRequest) unmarshaller.unmarshal(new StringReader(xmlMessage));

      // Process fraud check
      FraudCheckResponse response = fraudCheckEngine.check(request);

      // Marshal FraudCheckResponse → XML
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      StringWriter writer = new StringWriter();
      marshaller.marshal(response, writer);
      String responseXml = writer.toString();

      // Send response to fraud.response.queue
      jmsTemplate.convertAndSend(FRAUD_RESPONSE_QUEUE, responseXml);
      log.info(
          "Sent fraud check response for transaction {} to {}: {}",
          request.getTransactionId(),
          FRAUD_RESPONSE_QUEUE,
          response.getOutcome());

    } catch (Exception e) {
      log.error("Error processing fraud check request", e);
    }
  }
}
