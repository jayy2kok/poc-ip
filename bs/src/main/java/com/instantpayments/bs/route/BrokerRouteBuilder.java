package com.instantpayments.bs.route;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.instantpayments.bs.dto.xml.FraudCheckRequestXml;
import com.instantpayments.bs.dto.xml.FraudCheckResponseXml;
import com.instantpayments.common.QueueNames;
import com.instantpayments.common.dto.PaymentPayload;
import jakarta.xml.bind.JAXBContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

/**
 * Apache Camel routes for the Broker System. Replaces manual Spring JMS listeners and JmsTemplate
 * usage with declarative Camel route definitions.
 *
 * <p>Three routes are defined:
 *
 * <ul>
 *   <li><b>Sol1</b>: JMS (JSON) → JSON→XML conversion → JMS (XML) to FCS
 *   <li><b>Sol2</b>: REST controller invokes direct endpoint → JSON→XML conversion → JMS (XML) to
 *       FCS
 *   <li><b>Response</b>: JMS (XML from FCS) → XML→JSON conversion → JMS (JSON notification) to PPS
 * </ul>
 */
@Component
public class BrokerRouteBuilder extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    // JAXB data formats for XML marshalling/unmarshalling
    JAXBContext requestContext = JAXBContext.newInstance(FraudCheckRequestXml.class);
    JaxbDataFormat jaxbRequest = new JaxbDataFormat(requestContext);
    jaxbRequest.setPrettyPrint(true);

    JAXBContext responseContext = JAXBContext.newInstance(FraudCheckResponseXml.class);
    JaxbDataFormat jaxbResponse = new JaxbDataFormat(responseContext);

    // Jackson data formats with Java 8 date/time support
    JacksonDataFormat jacksonUnmarshal = new JacksonDataFormat(PaymentPayload.class);
    jacksonUnmarshal.addModule(new JavaTimeModule());
    jacksonUnmarshal.setDisableFeatures("WRITE_DATES_AS_TIMESTAMPS");

    JacksonDataFormat jacksonMarshal = new JacksonDataFormat();
    jacksonMarshal.addModule(new JavaTimeModule());
    jacksonMarshal.setDisableFeatures("WRITE_DATES_AS_TIMESTAMPS");

    // ─── Solution 1: JMS inbound (JSON from PPS) ────────────────────────
    from("jms:queue:" + QueueNames.PAYMENT_REQUEST_QUEUE)
        .routeId("sol1-payment-request")
        .autoStartup("{{camel.route.sol1.enabled:false}}")
        .log("Received payment request on " + QueueNames.PAYMENT_REQUEST_QUEUE)
        .unmarshal(jacksonUnmarshal)
        .bean("paymentMapper", "toFraudCheckRequestXml")
        .marshal(jaxbRequest)
        .convertBodyTo(String.class, "UTF-8")
        .to("jms:queue:" + QueueNames.FRAUD_REQUEST_QUEUE)
        .log("Forwarded fraud check request to " + QueueNames.FRAUD_REQUEST_QUEUE);

    // ─── Solution 2: REST inbound (direct endpoint) ─────────────────────
    from("direct:fraudCheckRest")
        .routeId("sol2-fraud-check-rest")
        .log("Processing REST fraud check request")
        .bean("paymentMapper", "toFraudCheckRequestXml")
        .marshal(jaxbRequest)
        .convertBodyTo(String.class, "UTF-8")
        .to("jms:queue:" + QueueNames.FRAUD_REQUEST_QUEUE)
        .log("Forwarded fraud check request to " + QueueNames.FRAUD_REQUEST_QUEUE);

    // ─── Response: JMS inbound (XML from FCS) ───────────────────────────
    from("jms:queue:" + QueueNames.FRAUD_RESPONSE_QUEUE)
        .routeId("fraud-response")
        .log("Received fraud response on " + QueueNames.FRAUD_RESPONSE_QUEUE)
        .unmarshal(jaxbResponse)
        .bean("paymentMapper", "toPaymentNotification")
        .marshal(jacksonMarshal)
        .convertBodyTo(String.class, "UTF-8")
        .to("jms:queue:" + QueueNames.PAYMENT_NOTIFICATION_QUEUE)
        .log("Sent payment notification to " + QueueNames.PAYMENT_NOTIFICATION_QUEUE);
  }
}
