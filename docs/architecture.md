# Instant Payments PoC — Architecture Document

> **Version**: 1.0  
> **Date**: 2026-03-04  
> **Status**: Draft

---

## 1. Introduction

### 1.1 Purpose
This document describes the architecture of the **Instant Payments PoC** — a fraud-check pipeline for bank payments. The system validates, brokers, and fraud-screens payment transactions before approval or rejection.

### 1.2 Scope
Four independent microservices communicate via **ActiveMQ Artemis** (JMS), **REST APIs**, and **Debezium CDC** to process and audit payments:

- **Payment Processing System (PPS)** — Receives, validates, persists, and finalizes payments
- **Broker System (BS)** — Mediates between PPS and FCS, converts JSON ↔ XML
- **Fraud Check System (FCS)** — Screens payments against configurable blacklists
- **Audit Service (AS)** — Captures payment data changes via CDC and provides audit trail

### 1.3 Solution Variants

| Aspect | Solution 1 | Solution 2 |
|---|---|---|
| PPS ↔ BS | JMS/JSON messaging | REST API/JSON |
| BS ↔ FCS | JMS/XML messaging | JMS/XML messaging |
| Intra-system | JMS messaging | JMS messaging |

Both solutions share maximum code via **Spring Profiles** (`sol1`, `sol2`).

---

## 2. System Context Diagram

```mermaid
graph TB
    User["User / Swagger UI"]

    subgraph "Instant Payments Platform"
        PPS["Payment Processing\nSystem (PPS)\n:8081"]
        BS["Broker System (BS)\n:8082"]
        FCS["Fraud Check\nSystem (FCS)\n:8083"]
        AS["Audit Service (AS)\n:8084"]
    end

    subgraph "Infrastructure"
        AMQ["ActiveMQ Artemis\n:61616"]
        PG["PostgreSQL\n:5432"]
        DBZ["Debezium Server\n:8085"]
    end

    User -->|"REST API / WebSocket"| PPS
    User -->|"REST API (Audit Logs)"| AS
    PPS -->|"JMS (Sol1) / REST (Sol2)"| BS
    PPS -->|"JPA"| PG
    AS -->|"JPA"| PG
    BS -->|"XML"| FCS
    BS -.->|"JMS"| AMQ
    FCS -.->|"JMS"| AMQ
    PG -.->|"WAL (CDC)"| DBZ
    DBZ -->|"HTTP POST"| AS
```

---

## 3. Component Architecture

### 3.1 Payment Processing System (PPS)

**Responsibilities:**
1. Receive payment requests (JSON) via REST API
2. Perform basic validation (ISO country codes, ISO currency codes, required fields)
3. Persist payments to PostgreSQL (status: `PENDING`)
4. Forward payment for fraud check (JMS queue for Sol1 / REST call for Sol2)
5. Receive fraud check results and update payment status (`APPROVED` / `REJECTED`)
6. Push real-time status updates to UI via WebSocket (STOMP)

> **Note:** Audit logging is **not** PPS's responsibility. Changes to the `payments` table are automatically captured via Debezium CDC and processed by the Audit Service (see §3.4).

```mermaid
graph TB
    subgraph "PPS Microservice [:8081]"
        direction TB

        subgraph "API Layer"
            PC["PaymentController\n(REST)"]
            WS["WebSocket\nEndpoint"]
        end

        subgraph "Service Layer"
            PS["PaymentService"]
            PV["PaymentValidator"]
        end

        subgraph "Data Layer"
            PR["PaymentRepository"]
        end

        subgraph "Integration Layer"
            CR_SOL1["Camel JMS Routes\n(Sol1 Profile)"]
            CR_SOL2["REST Client\n(Sol2 Profile)"]
            NL["Notification Listener\n(JMS)"]
        end
    end

    PC --> PS
    PS --> PV
    PS --> PR
    PS --> CR_SOL1
    PS --> CR_SOL2
    NL --> PS
    PS --> WS
```

### 3.2 Broker System (BS)

**Responsibilities:**
1. Receive fraud check requests from PPS (JMS/JSON for Sol1, REST/JSON for Sol2)
2. Convert JSON payment payload → XML fraud check request
3. Forward XML request to FCS via JMS queue
4. Receive XML fraud check response from FCS
5. Convert XML response → JSON notification
6. Send JSON notification back to PPS via JMS queue

```mermaid
graph TB
    subgraph "BS Microservice [:8082]"
        direction TB

        subgraph "Inbound"
            JMS_IN["JMS Listener\n(Sol1 - payment.request.queue)"]
            REST_IN["REST Controller\n(Sol2 - /api/v1/fraud-check)"]
        end

        subgraph "Processing"
            J2X["JSON → XML\nConverter"]
            X2J["XML → JSON\nConverter"]
            ROUTE["Camel Routes\n(Orchestration)"]
        end

        subgraph "Outbound"
            FCS_OUT["JMS Producer\n(fraud.request.queue)"]
            FCS_IN["JMS Listener\n(fraud.response.queue)"]
            PPS_OUT["JMS Producer\n(payment.notification.queue)"]
        end
    end

    JMS_IN --> J2X
    REST_IN --> J2X
    J2X --> ROUTE
    ROUTE --> FCS_OUT
    FCS_IN --> X2J
    X2J --> PPS_OUT
```

### 3.3 Fraud Check System (FCS)

**Responsibilities:**
1. Receive fraud check requests in XML from BS via JMS
2. Validate payment against configurable blacklists (names, countries, banks, payment instructions)
3. Return fraud check result (APPROVED/REJECTED) in XML via JMS

```mermaid
graph TB
    subgraph "FCS Microservice [:8083]"
        direction TB

        subgraph "Integration"
            JMS_IN["JMS Listener\n(fraud.request.queue)"]
            JMS_OUT["JMS Producer\n(fraud.response.queue)"]
        end

        subgraph "Processing"
            FCE["FraudCheckEngine"]
            BLS["BlacklistService"]
        end

        subgraph "Configuration"
            BL_CFG["blacklist.yml\n(Externalized Config)"]
        end
    end

    JMS_IN --> FCE
    FCE --> BLS
    BLS --> BL_CFG
    FCE --> JMS_OUT
```

### 3.4 Audit Service (AS)

**Responsibilities:**
1. Receive CDC change events from Debezium Server via HTTP POST
2. Parse Debezium envelope (operation type, before/after row state)
3. Persist raw change records to `audit_logs` table
4. Expose REST API for querying audit history with filtering

```mermaid
graph TB
    subgraph "AS Microservice [:8084]"
        direction TB

        subgraph "API Layer"
            CDC_IN["CDC Ingest Endpoint\n(POST /api/v1/cdc/payments)"]
            AQ["Audit Query Controller\n(GET /api/v1/audit-logs)"]
        end

        subgraph "Service Layer"
            AUS["AuditService"]
            CDP["CdcEventParser"]
        end

        subgraph "Data Layer"
            AR["AuditRepository"]
        end
    end

    CDC_IN --> CDP
    CDP --> AUS
    AUS --> AR
    AQ --> AUS
```

### 3.5 Debezium Server (CDC Infrastructure)

**Responsibilities:**
1. Connect to PostgreSQL via logical replication (WAL)
2. Monitor the `payments` table for INSERT, UPDATE, DELETE operations
3. Emit change events as JSON to the Audit Service via HTTP sink

**Configuration** (`application.properties`):
```properties
# Source — PostgreSQL
debezium.source.connector.class=io.debezium.connector.postgresql.PostgresConnector
debezium.source.database.hostname=postgres
debezium.source.database.port=5432
debezium.source.database.user=debezium
debezium.source.database.password=dbz_pass
debezium.source.database.dbname=instantpayments
debezium.source.table.include.list=public.payments
debezium.source.plugin.name=pgoutput
debezium.source.topic.prefix=cdc

# Sink — HTTP to Audit Service
debezium.sink.type=http
debezium.sink.http.url=http://audit-service:8084/api/v1/cdc/payments
debezium.sink.http.timeout.ms=5000

# Transforms
debezium.transforms=unwrap
debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState
debezium.transforms.unwrap.add.fields=op,table,source.ts_ms
debezium.transforms.unwrap.delete.handling.mode=rewrite
```

> **PostgreSQL prerequisite:** `wal_level` must be set to `logical` in `postgresql.conf`.

---

## 4. Data Flow Diagrams

### 4.1 Solution 1 — Full JMS Flow (PPS ↔ BS via Messaging)

```mermaid
sequenceDiagram
    participant UI as Swagger UI
    participant PPS as PPS [:8081]
    participant DB as PostgreSQL
    participant DBZ as Debezium Server
    participant AS as Audit Service [:8084]
    participant AMQ as Artemis
    participant BS as BS [:8082]
    participant FCS as FCS [:8083]

    Note over UI,FCS: Payment Submission
    UI->>PPS: POST /api/v1/payments (JSON)
    PPS->>PPS: Validate (ISO codes, required fields)
    alt Validation Fails
        PPS-->>UI: 400 Bad Request + validation errors
    end
    PPS->>DB: INSERT payment (status=PENDING)
    PPS-->>UI: 202 Accepted {transactionId, status: PENDING}

    Note over DB,AS: CDC — Audit Capture (async)
    DB->>DBZ: WAL event (INSERT on payments)
    DBZ->>AS: POST /api/v1/cdc/payments (change event)
    AS->>DB: INSERT audit_logs (operation=INSERT, after_state={...})

    Note over PPS,FCS: Fraud Check Pipeline
    PPS->>AMQ: payment.request.queue (JSON)
    AMQ->>BS: Consume payment request
    BS->>BS: Convert JSON → XML
    BS->>AMQ: fraud.request.queue (XML)
    AMQ->>FCS: Consume fraud request
    FCS->>FCS: Check blacklists (name, country, bank, instruction)
    alt Blacklist Match Found
        FCS->>AMQ: fraud.response.queue (XML: REJECTED, "Suspicious payment")
    else No Match
        FCS->>AMQ: fraud.response.queue (XML: APPROVED, "Nothing found, all okay")
    end

    Note over PPS,FCS: Result Propagation
    AMQ->>BS: Consume fraud response (XML)
    BS->>BS: Convert XML → JSON
    BS->>AMQ: payment.notification.queue (JSON)
    AMQ->>PPS: Consume notification
    PPS->>DB: UPDATE payment status (APPROVED/REJECTED)
    PPS->>UI: WebSocket push (status update)

    Note over DB,AS: CDC — Audit Capture (async)
    DB->>DBZ: WAL event (UPDATE on payments)
    DBZ->>AS: POST /api/v1/cdc/payments (change event)
    AS->>DB: INSERT audit_logs (operation=UPDATE, before/after)
```

### 4.2 Solution 2 — REST + JMS Flow (PPS → BS via REST)

```mermaid
sequenceDiagram
    participant UI as Swagger UI
    participant PPS as PPS [:8081]
    participant DB as PostgreSQL
    participant DBZ as Debezium Server
    participant AS as Audit Service [:8084]
    participant BS as BS [:8082]
    participant AMQ as Artemis
    participant FCS as FCS [:8083]

    Note over UI,FCS: Payment Submission (same as Sol1)
    UI->>PPS: POST /api/v1/payments (JSON)
    PPS->>PPS: Validate
    PPS->>DB: INSERT payment (status=PENDING)
    PPS-->>UI: 202 Accepted {transactionId, status: PENDING}

    Note over DB,AS: CDC — Audit Capture (async, same as Sol1)
    DB->>DBZ: WAL event (INSERT)
    DBZ->>AS: POST /api/v1/cdc/payments
    AS->>DB: INSERT audit_logs

    Note over PPS,FCS: Fraud Check via REST → JMS
    PPS->>BS: POST /api/v1/fraud-check (JSON via REST)
    BS-->>PPS: 202 Accepted
    BS->>BS: Convert JSON → XML
    BS->>AMQ: fraud.request.queue (XML)
    AMQ->>FCS: Consume fraud request
    FCS->>FCS: Check blacklists
    FCS->>AMQ: fraud.response.queue (XML)

    Note over PPS,FCS: Result Propagation (same as Sol1)
    AMQ->>BS: Consume fraud response (XML)
    BS->>BS: Convert XML → JSON
    BS->>AMQ: payment.notification.queue (JSON)
    AMQ->>PPS: Consume notification
    PPS->>DB: UPDATE payment status
    PPS->>UI: WebSocket push

    Note over DB,AS: CDC — Audit Capture (async)
    DB->>DBZ: WAL event (UPDATE)
    DBZ->>AS: POST /api/v1/cdc/payments
    AS->>DB: INSERT audit_logs
```

---

## 5. Data Models

### 5.1 Payment Payload (JSON — PPS Inbound/Outbound)

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "payerName": "Munster Muller",
  "payerBank": "Bank of America",
  "payerCountryCode": "USA",
  "payerAccount": "1234567890",
  "payeeName": "John Doe",
  "payeeBank": "BNP Paribas",
  "payeeCountryCode": "DEU",
  "payeeAccount": "0987654321",
  "paymentInstruction": "Loan Repayment",
  "executionDate": "2026-03-04",
  "amount": 1500.00,
  "currency": "EUR",
  "creationTimestamp": "2026-03-04T12:30:00.000Z"
}
```

### 5.2 Fraud Check Request (XML — BS → FCS)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FraudCheckRequest xmlns="http://poc.instantpayments/fraud">
    <transactionId>550e8400-e29b-41d4-a716-446655440000</transactionId>
    <payer>
        <name>Munster Muller</name>
        <bank>Bank of America</bank>
        <countryCode>USA</countryCode>
        <account>1234567890</account>
    </payer>
    <payee>
        <name>John Doe</name>
        <bank>BNP Paribas</bank>
        <countryCode>DEU</countryCode>
        <account>0987654321</account>
    </payee>
    <paymentInstruction>Loan Repayment</paymentInstruction>
    <executionDate>2026-03-04</executionDate>
    <amount>1500.00</amount>
    <currency>EUR</currency>
</FraudCheckRequest>
```

### 5.3 Fraud Check Response (XML — FCS → BS)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FraudCheckResponse xmlns="http://poc.instantpayments/fraud">
    <transactionId>550e8400-e29b-41d4-a716-446655440000</transactionId>
    <outcome>APPROVED</outcome>
    <message>Nothing found, all okay</message>
    <checkedAt>2026-03-04T12:30:05.000Z</checkedAt>
</FraudCheckResponse>
```

### 5.4 Payment Notification (JSON — BS → PPS)

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "outcome": "APPROVED",
  "message": "Nothing found, all okay",
  "checkedAt": "2026-03-04T12:30:05.000Z"
}
```

### 5.5 CDC Change Event (JSON — Debezium Server → Audit Service)

**INSERT event** (payment created):
```json
{
  "op": "c",
  "ts_ms": 1709554200000,
  "source": {
    "table": "payments",
    "db": "instantpayments",
    "lsn": 33495432
  },
  "before": null,
  "after": {
    "id": 1,
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "payer_name": "Munster Muller",
    "payer_bank": "Bank of America",
    "payer_country": "USA",
    "payer_account": "1234567890",
    "payee_name": "John Doe",
    "payee_bank": "BNP Paribas",
    "payee_country": "DEU",
    "payee_account": "0987654321",
    "payment_instruction": "Loan Repayment",
    "execution_date": "2026-03-04",
    "amount": 1500.00,
    "currency": "EUR",
    "status": "PENDING",
    "fraud_message": null,
    "created_at": "2026-03-04T12:30:00.000Z",
    "updated_at": "2026-03-04T12:30:00.000Z"
  }
}
```

**UPDATE event** (status changed after fraud check):
```json
{
  "op": "u",
  "ts_ms": 1709554205000,
  "source": {
    "table": "payments",
    "db": "instantpayments",
    "lsn": 33495500
  },
  "before": {
    "id": 1,
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "fraud_message": null,
    "updated_at": "2026-03-04T12:30:00.000Z"
  },
  "after": {
    "id": 1,
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "APPROVED",
    "fraud_message": "Nothing found, all okay",
    "updated_at": "2026-03-04T12:30:05.000Z"
  }
}
```

**Operation codes:** `c` = INSERT, `u` = UPDATE, `d` = DELETE

---

## 6. Database Schema

```mermaid
erDiagram
    payments {
        UUID transaction_id PK
        VARCHAR payer_name
        VARCHAR payer_bank
        CHAR payer_country
        VARCHAR payer_account
        VARCHAR payee_name
        VARCHAR payee_bank
        CHAR payee_country
        VARCHAR payee_account
        TEXT payment_instruction
        DATE execution_date
        DECIMAL amount
        CHAR currency
        VARCHAR status
        TEXT fraud_message
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    audit_logs {
        BIGSERIAL id PK
        UUID transaction_id FK
        VARCHAR operation
        JSONB before_state
        JSONB after_state
        TEXT_ARRAY changed_fields
        TIMESTAMPTZ source_ts
        TIMESTAMPTZ captured_at
    }

    payments ||--o{ audit_logs : "CDC captures changes"
```

### 6.1 `payments` Table

```sql
CREATE TABLE payments (
    transaction_id  UUID            PRIMARY KEY,
    payer_name      VARCHAR(255)    NOT NULL,
    payer_bank      VARCHAR(255)    NOT NULL,
    payer_country   CHAR(3)         NOT NULL,
    payer_account   VARCHAR(50)     NOT NULL,
    payee_name      VARCHAR(255)    NOT NULL,
    payee_bank      VARCHAR(255)    NOT NULL,
    payee_country   CHAR(3)         NOT NULL,
    payee_account   VARCHAR(50)     NOT NULL,
    payment_instruction TEXT,
    execution_date  DATE            NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    currency        CHAR(3)         NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    fraud_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_status ON payments(status);
```

### 6.2 `audit_logs` Table (CDC Pattern)

```sql
CREATE TABLE audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  UUID            NOT NULL,
    operation       VARCHAR(10)     NOT NULL,   -- INSERT, UPDATE, DELETE
    before_state    JSONB,                      -- null for INSERT
    after_state     JSONB,                      -- null for DELETE
    changed_fields  TEXT[],                     -- columns that changed (UPDATEs only)
    source_ts       TIMESTAMPTZ     NOT NULL,   -- DB commit time from Debezium
    captured_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_transaction_id ON audit_logs(transaction_id);
CREATE INDEX idx_audit_operation ON audit_logs(operation);
CREATE INDEX idx_audit_source_ts ON audit_logs(source_ts);
```

> **Note:** `before_state` and `after_state` store full JSONB snapshots of the `payments` row, enabling before/after comparison in the UI.

### 6.3 PostgreSQL Configuration for CDC

```sql
-- Required: enable logical replication
ALTER SYSTEM SET wal_level = 'logical';

-- Create a replication user for Debezium
CREATE ROLE debezium WITH REPLICATION LOGIN PASSWORD 'dbz_pass';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;

-- Ensure REPLICA IDENTITY is FULL for before-state capture
ALTER TABLE payments REPLICA IDENTITY FULL;
```

---

## 7. Messaging & CDC Topology

### 7.1 JMS Queues (ActiveMQ Artemis)

```mermaid
graph LR
    subgraph "ActiveMQ Artemis"
        Q1["payment.request.queue\n(JSON)"]
        Q2["fraud.request.queue\n(XML)"]
        Q3["fraud.response.queue\n(XML)"]
        Q4["payment.notification.queue\n(JSON)"]
    end

    PPS["PPS"] -->|"produce (Sol1)"| Q1
    Q1 -->|"consume"| BS["BS"]
    BS -->|"produce"| Q2
    Q2 -->|"consume"| FCS["FCS"]
    FCS -->|"produce"| Q3
    Q3 -->|"consume"| BS
    BS -->|"produce"| Q4
    Q4 -->|"consume"| PPS
```

| Name | Type | Format | Producer | Consumer |
|---|---|---|---|---|
| `payment.request.queue` | Queue | JSON | PPS (Sol1) | BS |
| `fraud.request.queue` | Queue | XML | BS | FCS |
| `fraud.response.queue` | Queue | XML | FCS | BS |
| `payment.notification.queue` | Queue | JSON | BS | PPS |

### 7.2 CDC Flow (Debezium Server)

```mermaid
graph LR
    PG["PostgreSQL\n(WAL)"] -->|"Logical Replication"| DBZ["Debezium Server\n:8085"]
    DBZ -->|"HTTP POST\n(JSON change events)"| AS["Audit Service\n:8084"]
    AS -->|"JPA"| PG_AUDIT["PostgreSQL\n(audit_logs table)"]
```

| Source | Mechanism | Format | Destination |
|---|---|---|---|
| `payments` table (WAL) | PostgreSQL logical replication | Debezium envelope (JSON) | Debezium Server |
| Debezium Server | HTTP POST | JSON change event | Audit Service (`/api/v1/cdc/payments`) |

---

## 8. Code Reuse Strategy (Spring Profiles)

```mermaid
graph TB
    subgraph "Shared Code (Always Active)"
        C["common module"]
        VAL["PaymentValidator"]
        DB_LAYER["JPA Entity + Repository"]
        SVC["PaymentService"]
        WS["WebSocket"]
        FCS_ALL["FCS (entire system)"]
        BS_CORE["BS JSON↔XML Converter"]
        BS_FCS["BS ↔ FCS JMS Routes"]
        AS_ALL["AS (entire system)"]
    end

    subgraph "Profile: sol1"
        PPS_JMS["PPS JMS Routes\n(payment.request.queue)"]
        BS_JMS_IN["BS JMS Inbound\n(from PPS)"]
    end

    subgraph "Profile: sol2"
        PPS_REST_OUT["PPS REST Client\n(calls BS)"]
        BS_REST_IN["BS REST Controller\n(/api/v1/fraud-check)"]
    end
```

**Activation:**
```bash
# Solution 1 (full JMS)
java -jar pps.jar --spring.profiles.active=sol1
java -jar bs.jar --spring.profiles.active=sol1

# Solution 2 (REST + JMS)
java -jar pps.jar --spring.profiles.active=sol2
java -jar bs.jar --spring.profiles.active=sol2

# FCS — no profile needed (identical for both)
java -jar fcs.jar

# AS — no profile needed (standalone CDC consumer)
java -jar as.jar
```

---

## 9. Blacklist Configuration

Externalized via `blacklist.yml` in FCS, loaded as `@ConfigurationProperties`:

```yaml
blacklist:
  names:
    - "Mark Imaginary"
    - "Govind Real"
    - "Shakil Maybe"
    - "Chang Imagine"
  countries:
    - "CUB"
    - "IRQ"
    - "IRN"
    - "PRK"
    - "SDN"
    - "SYR"
  banks:
    - "BANK OF KUNLUN"
    - "KARAMAY CITY COMMERCIAL BANK"
  paymentInstructions:
    - "Artillery Procurement"
    - "Lethal Chemicals payment"
```

**Matching Logic:**
- Case-insensitive comparison on all fields
- Checks: payer name, payee name, payer country, payee country, payer bank, payee bank, payment instruction
- If **any** match → `REJECTED` with message `"Suspicious payment"`
- If **no** match → `APPROVED` with message `"Nothing found, all okay"`

---

## 10. Deployment Architecture

```mermaid
graph TB
    subgraph "Docker Compose"
        PG["PostgreSQL :5432\n(wal_level=logical)"]
        AMQ["Artemis :61616 / :8161"]
        DBZ["Debezium Server :8085"]
        PPS["PPS :8081\n(Swagger UI)"]
        BS["BS :8082\n(Swagger UI)"]
        FCS["FCS :8083"]
        AS["Audit Service :8084\n(Swagger UI)"]
    end

    PPS --> PG
    AS --> PG
    PPS --> AMQ
    BS --> AMQ
    FCS --> AMQ
    PG -.->|"WAL"| DBZ
    DBZ -->|"HTTP"| AS
```

**Port Mapping:**

| Service | Port | Description |
|---|---|---|
| PostgreSQL | 5432 | Database (wal_level=logical) |
| Artemis (JMS) | 61616 | AMQP/OpenWire |
| Artemis (Console) | 8161 | Web management console |
| PPS | 8081 | Payment Processing System (Swagger UI: `/swagger-ui.html`) |
| BS | 8082 | Broker System (Swagger UI: `/swagger-ui.html`) |
| FCS | 8083 | Fraud Check System |
| Audit Service | 8084 | CDC Audit Service (Swagger UI: `/swagger-ui.html`) |
| Debezium Server | 8085 | CDC connector (PostgreSQL → HTTP) |

---

## 11. Technology Stack Summary

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Build | Maven (multi-module) | 3.9+ |
| Framework | Spring Boot | 3.2.x |
| Integration | Apache Camel (Spring Boot Starter) | 4.x |
| Messaging | ActiveMQ Artemis | 2.31+ |
| CDC | Debezium Server (PostgreSQL connector) | 2.5+ |
| Database | PostgreSQL (wal_level=logical) | 15+ |
| ORM | Spring Data JPA / Hibernate | — |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.x |
| Code Gen | OpenAPI Generator Maven Plugin | 7.4.0 |
| WebSocket | Spring WebSocket + STOMP | — |
| JSON | Jackson | — |
| XML | JAXB | — |
| Testing | JUnit 5, Mockito, Testcontainers | — |
| Coverage | JaCoCo (95% target) | — |
| Containers | Docker, Docker Compose | — |
| Logging | SLF4J + Logback (async) | — |

---

## 12. API-First Approach & Swagger UI

All service APIs follow an **API-first** development approach:

1. **OpenAPI specs** are the single source of truth (`docs/api/*.yaml`)
2. **OpenAPI Generator** (Maven plugin) generates Spring interfaces and DTOs at build time
3. Controllers implement the generated interfaces — ensuring spec-code consistency
4. **Swagger UI** is available on each service at `/swagger-ui.html`, rendering directly from the YAML spec

| Service | Swagger UI URL |
|---|---|
| PPS | `http://localhost:8081/swagger-ui.html` |
| BS | `http://localhost:8082/swagger-ui.html` |
| AS | `http://localhost:8084/swagger-ui.html` |
