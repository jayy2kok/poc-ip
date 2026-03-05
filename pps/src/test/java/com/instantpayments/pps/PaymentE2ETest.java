package com.instantpayments.pps;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * End-to-end integration tests for the Payment Processing System.
 *
 * <p><b>Prerequisites:</b> All Docker containers must be running ({@code docker-compose up -d})
 * before executing these tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PaymentE2ETest {

  private static final String PPS_BASE = "http://localhost:8081";

  /** Stored from test #1 for reuse in subsequent tests. */
  private String approvedTxId;

  @BeforeAll
  void setup() {
    RestAssured.baseURI = PPS_BASE;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  // ───────────────── 1. Submit valid payment → APPROVED ─────────────────

  @Test
  @Order(1)
  @DisplayName("Submit valid payment → 202 Accepted, transitions to APPROVED")
  void submitValidPayment_shouldBeApproved() {
    String body =
        """
                {
                    "payerName": "E2E Valid User",
                    "payerBank": "Standard Chartered",
                    "payerCountryCode": "USA",
                    "payerAccount": "1111111111",
                    "payeeName": "E2E Receiver",
                    "payeeBank": "Deutsche Bank",
                    "payeeCountryCode": "DEU",
                    "payeeAccount": "2222222222",
                    "paymentInstruction": "Invoice payment 12345",
                    "executionDate": "2026-03-05",
                    "amount": 1500.00,
                    "currency": "EUR"
                }
                """;

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(202)
            .body("transactionId", notNullValue())
            .body("status", equalTo("PENDING"))
            .body("message", containsString("Payment accepted"))
            .extract()
            .response();

    approvedTxId = response.jsonPath().getString("transactionId");

    // Wait for the async fraud-check pipeline to finish
    await()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                given()
                    .when()
                    .get("/api/v1/payments/{id}/status", approvedTxId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("APPROVED")));
  }

  // ───────────────── 2. Blacklisted payer name → REJECTED ─────────────────

  @Test
  @Order(2)
  @DisplayName("Blacklisted payer name → payment REJECTED")
  void submitBlacklistedPayerName_shouldBeRejected() {
    String body =
        """
                {
                    "payerName": "Mark Imaginary",
                    "payerBank": "Test Bank",
                    "payerCountryCode": "USA",
                    "payerAccount": "3333333333",
                    "payeeName": "Receiver",
                    "payeeBank": "Deutsche Bank",
                    "payeeCountryCode": "DEU",
                    "payeeAccount": "4444444444",
                    "paymentInstruction": "Normal payment",
                    "executionDate": "2026-03-05",
                    "amount": 200.00,
                    "currency": "USD"
                }
                """;

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(202)
            .extract()
            .response();

    String txId = response.jsonPath().getString("transactionId");

    await()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                given()
                    .when()
                    .get("/api/v1/payments/{id}/status", txId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("REJECTED")));
  }

  // ───────────────── 3. Blacklisted country → REJECTED ─────────────────

  @Test
  @Order(3)
  @DisplayName("Blacklisted payer country → payment REJECTED")
  void submitBlacklistedCountry_shouldBeRejected() {
    String body =
        """
                {
                    "payerName": "Legit User",
                    "payerBank": "Test Bank",
                    "payerCountryCode": "IRN",
                    "payerAccount": "5555555555",
                    "payeeName": "Receiver",
                    "payeeBank": "Deutsche Bank",
                    "payeeCountryCode": "DEU",
                    "payeeAccount": "6666666666",
                    "paymentInstruction": "Normal payment",
                    "executionDate": "2026-03-05",
                    "amount": 300.00,
                    "currency": "GBP"
                }
                """;

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(202)
            .extract()
            .response();

    String txId = response.jsonPath().getString("transactionId");

    await()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                given()
                    .when()
                    .get("/api/v1/payments/{id}/status", txId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("REJECTED")));
  }

  // ───────────────── 4. Missing required fields → 400 ─────────────────

  @Test
  @Order(4)
  @DisplayName("Missing required fields → 400 Bad Request")
  void submitInvalidPayment_shouldReturn400() {
    String body =
        """
                {
                    "payerName": "",
                    "payerBank": "",
                    "amount": -100
                }
                """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(400);
  }

  // ───────────────── 5. Get payment by transactionId ─────────────────

  @Test
  @Order(5)
  @DisplayName("GET payment by transactionId → returns full details")
  void getPaymentByTransactionId_shouldReturnDetails() {
    Assumptions.assumeTrue(approvedTxId != null, "Requires test #1 to pass first");

    given()
        .when()
        .get("/api/v1/payments/{id}", approvedTxId)
        .then()
        .statusCode(200)
        .body("transactionId", equalTo(approvedTxId))
        .body("payerName", equalTo("E2E Valid User"))
        .body("status", equalTo("APPROVED"))
        .body("amount", equalTo(1500.0f))
        .body("currency", equalTo("EUR"));
  }

  // ───────────────── 6. Get payment status ─────────────────

  @Test
  @Order(6)
  @DisplayName("GET payment status → returns status and updatedAt")
  void getPaymentStatus_shouldReturnStatus() {
    Assumptions.assumeTrue(approvedTxId != null, "Requires test #1 to pass first");

    given()
        .when()
        .get("/api/v1/payments/{id}/status", approvedTxId)
        .then()
        .statusCode(200)
        .body("transactionId", equalTo(approvedTxId))
        .body("status", equalTo("APPROVED"))
        .body("updatedAt", notNullValue());
  }

  // ───────────────── 7. List payments (paginated) ─────────────────

  @Test
  @Order(7)
  @DisplayName("GET payments list → paginated response with content")
  void listPayments_shouldReturnPaginatedResults() {
    given()
        .queryParam("page", 0)
        .queryParam("size", 5)
        .when()
        .get("/api/v1/payments")
        .then()
        .statusCode(200)
        .body("content", not(empty()))
        .body("page", equalTo(0))
        .body("size", equalTo(5))
        .body("totalElements", greaterThanOrEqualTo(1));
  }

  // ───────────────── 8. Non-existent payment → 404 ─────────────────

  @Test
  @Order(8)
  @DisplayName("GET non-existent payment → 404 Not Found")
  void getNonExistentPayment_shouldReturn404() {
    String randomUuid = UUID.randomUUID().toString();

    given().when().get("/api/v1/payments/{id}", randomUuid).then().statusCode(404);
  }
}
