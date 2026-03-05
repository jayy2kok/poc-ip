package com.instantpayments.as;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
 * End-to-end integration tests for the Audit Service.
 *
 * <p>Tests submit a payment through PPS (port 8081), wait for CDC to propagate the change to the
 * Audit Service (port 8084), and then verify the audit trail through the Audit Service API.
 *
 * <p><b>Prerequisites:</b> All Docker containers must be running ({@code docker-compose up -d}),
 * including Debezium for CDC.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuditE2ETest {

  private static final String PPS_BASE = "http://localhost:8081";
  private static final String AS_BASE = "http://localhost:8084";

  /** Transaction ID created by test #1, reused in subsequent tests. */
  private String transactionId;

  @BeforeAll
  void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  // ───── 1. Submit payment → CDC audit logs created (INSERT + UPDATE) ─────

  @Test
  @Order(1)
  @DisplayName("Submit payment via PPS → CDC creates audit log entries")
  void submitPayment_cdcShouldCreateAuditLogs() {
    String body =
        """
                {
                    "payerName": "Audit E2E User",
                    "payerBank": "Standard Chartered",
                    "payerCountryCode": "USA",
                    "payerAccount": "7777777777",
                    "payeeName": "Audit Receiver",
                    "payeeBank": "Deutsche Bank",
                    "payeeCountryCode": "DEU",
                    "payeeAccount": "8888888888",
                    "paymentInstruction": "Audit trail test",
                    "executionDate": "2026-03-05",
                    "amount": 2500.00,
                    "currency": "EUR"
                }
                """;

    // Submit via PPS
    Response ppsResponse =
        given()
            .baseUri(PPS_BASE)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(202)
            .body("transactionId", notNullValue())
            .extract()
            .response();

    transactionId = ppsResponse.jsonPath().getString("transactionId");

    // Wait for PPS to complete processing (PENDING → APPROVED)
    await()
        .atMost(15, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                given()
                    .baseUri(PPS_BASE)
                    .when()
                    .get("/api/v1/payments/{id}/status", transactionId)
                    .then()
                    .body("status", equalTo("APPROVED")));

    // Wait for CDC to deliver audit logs (INSERT + UPDATE)
    await()
        .atMost(20, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                given()
                    .baseUri(AS_BASE)
                    .queryParam("transactionId", transactionId)
                    .when()
                    .get("/api/v1/audit-logs")
                    .then()
                    .statusCode(200)
                    .body("totalElements", greaterThanOrEqualTo(2)));
  }

  // ───── 2. List audit logs (paginated) ─────

  @Test
  @Order(2)
  @DisplayName("List audit logs → paginated response with correct structure")
  void listAuditLogs_shouldReturnPaginatedResults() {
    Assumptions.assumeTrue(transactionId != null, "Requires test #1 to pass first");

    given()
        .baseUri(AS_BASE)
        .queryParam("transactionId", transactionId)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/v1/audit-logs")
        .then()
        .statusCode(200)
        .body("content", not(empty()))
        .body("content[0].transactionId", equalTo(transactionId))
        .body("content[0].operation", anyOf(equalTo("INSERT"), equalTo("UPDATE")))
        .body("page", equalTo(0))
        .body("size", equalTo(10))
        .body("totalElements", greaterThanOrEqualTo(2));
  }

  // ───── 3. Get change history for transaction ─────

  @Test
  @Order(3)
  @DisplayName("Get change history → ordered list of audit entries")
  void getChangeHistory_shouldReturnOrderedEntries() {
    Assumptions.assumeTrue(transactionId != null, "Requires test #1 to pass first");

    given()
        .baseUri(AS_BASE)
        .when()
        .get("/api/v1/audit-logs/{txId}", transactionId)
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(2)))
        .body("[0].operation", equalTo("INSERT"))
        .body("[0].transactionId", equalTo(transactionId))
        .body("[0].afterState", notNullValue());
  }

  // ───── 4. Get transaction diff ─────

  @Test
  @Order(4)
  @DisplayName("Get transaction diff → shows status change PENDING → APPROVED")
  void getTransactionDiff_shouldShowStatusChange() {
    Assumptions.assumeTrue(transactionId != null, "Requires test #1 to pass first");

    given()
        .baseUri(AS_BASE)
        .when()
        .get("/api/v1/audit-logs/{txId}/diff", transactionId)
        .then()
        .statusCode(200)
        .body("$", not(empty()))
        .body("[0].operation", equalTo("UPDATE"))
        .body("[0].changes", not(empty()))
        .body("[0].changes.find { it.field == 'status' }.oldValue", equalTo("PENDING"))
        .body("[0].changes.find { it.field == 'status' }.newValue", equalTo("APPROVED"));
  }

  // ───── 5. Query non-existent transaction → empty / 404 ─────

  @Test
  @Order(5)
  @DisplayName("Query non-existent transaction → empty result or 404")
  void queryNonExistentTransaction_shouldReturnEmptyOr404() {
    String randomUuid = UUID.randomUUID().toString();

    // Audit logs endpoint returns empty page
    given()
        .baseUri(AS_BASE)
        .queryParam("transactionId", randomUuid)
        .when()
        .get("/api/v1/audit-logs")
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(0))
        .body("content", empty());

    // History endpoint returns 404
    given()
        .baseUri(AS_BASE)
        .when()
        .get("/api/v1/audit-logs/{txId}", randomUuid)
        .then()
        .statusCode(404);
  }
}
