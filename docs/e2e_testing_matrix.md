# End-to-End (E2E) Test Scenarios Matrix

This document provides a matrix mapping the supported E2E test scenarios to their corresponding automated test implementations across the system's domains (Payment Processing and Audit Trail).

## 1. Domain: Payment Processing (PPS)

| Scenario ID | Category | Scenario Description | Expected Outcome | Mapped Automated E2E Test | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PPS-E2E-01** | **Happy Path** | Submit a valid payment with all required fields properly populated. | HTTP 202 Accepted. The payment transitions from PENDING to APPROVED within a few seconds. | `PaymentE2ETest.submitValidPayment_shouldBeApproved()` | ✅ Implemented |
| **PPS-E2E-02** | **Validation** | Submit an invalid payment payload (e.g., missing required fields, completely empty payload). | HTTP 400 Bad Request. | `PaymentE2ETest.submitInvalidPayment_shouldReturn400()` | ✅ Implemented |
| **PPS-E2E-03** | **Compliance** | Submit a payment with a blacklisted payer name (e.g., "Osama Bin Laden"). | HTTP 202 Accepted initially, but the payment transitions to REJECTED by the Broker System. | `PaymentE2ETest.submitBlacklistedPayerName_shouldBeRejected()` | ✅ Implemented |
| **PPS-E2E-04** | **Compliance** | Submit a payment originating from or destined to a blacklisted country (e.g., "SYR"). | HTTP 202 Accepted initially, but the payment transitions to REJECTED by the Broker System. | `PaymentE2ETest.submitBlacklistedCountry_shouldBeRejected()` | ✅ Implemented |
| **PPS-E2E-05** | **Retrieval** | Fetch details of an existing payment using its transaction ID. | HTTP 200 OK with the corresponding payment details. | `PaymentE2ETest.getPaymentByTransactionId_shouldReturnDetails()` | ✅ Implemented |
| **PPS-E2E-06** | **Retrieval** | Query a non-existent payment transaction ID. | HTTP 404 Not Found. | `PaymentE2ETest.getNonExistentPayment_shouldReturn404()` | ✅ Implemented |
| **PPS-E2E-07** | **Retrieval** | Fetch the status alone of an existing payment transaction. | HTTP 200 OK with the target payment's current status string. | `PaymentE2ETest.getPaymentStatus_shouldReturnStatus()` | ✅ Implemented |
| **PPS-E2E-08** | **List/Pagination**| Retrieve a paginated list of all submitted payments. | HTTP 200 OK with an array of payments, correctly adhering to page and size limits. | `PaymentE2ETest.listPayments_shouldReturnPaginatedResults()` | ✅ Implemented |
| **PPS-E2E-09** | **Push Notifications**| Subscribe to a WebSocket topic and listen for real-time payment status updates. | STOMP payload received containing the `transactionId` and the updated `status` (e.g., APPROVED or REJECTED). | `PaymentE2ETest.testPushNotifications_shouldReceiveUpdates()` | ✅ Implemented |


## 2. Domain: Audit Trail & CDC (AS)

| Scenario ID | Category | Scenario Description | Expected Outcome | Mapped Automated E2E Test | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AS-E2E-01** | **CDC Pipeline** | Submit a payment via PPS and observe PostgreSQL CDC capturing and propagating changes to the Audit Service. | The PPS payment triggers Debezium CDC events, resulting in at least an `INSERT` and `UPDATE` log correctly appearing in the Audit DB. | `AuditE2ETest.submitPayment_cdcShouldCreateAuditLogs()` | ✅ Implemented |
| **AS-E2E-02** | **Retrieval** | Fetch a paginated list of audit logs relating to a single existing transaction ID. | HTTP 200 OK with an array of correct operational logs (e.g., INSERT, UPDATE) scoped to that transaction ID. | `AuditE2ETest.listAuditLogs_shouldReturnPaginatedResults()` | ✅ Implemented |
| **AS-E2E-03** | **History Trace**| Retrieve the chronologically ordered change history for a given transaction ID. | HTTP 200 OK with the list of historical entries ordered consecutively, starting from INSERT. | `AuditE2ETest.getChangeHistory_shouldReturnOrderedEntries()` | ✅ Implemented |
| **AS-E2E-04** | **Data Diff** | Retrieve a diff showing exact modifications made during an UPDATE event. | HTTP 200 OK with a payload detailing which particular fields transitioned (e.g., `status` from PENDING to APPROVED). | `AuditE2ETest.getTransactionDiff_shouldShowStatusChange()` | ✅ Implemented |
| **AS-E2E-05** | **Retrieval** | Query for audit histories/logs of a randomly generated, non-existent transaction. | HTTP 200 OK (Empty Page) on list endpoints, and HTTP 404 Not Found on specific history endpoints. | `AuditE2ETest.queryNonExistentTransaction_shouldReturnEmptyOr404()` | ✅ Implemented |
