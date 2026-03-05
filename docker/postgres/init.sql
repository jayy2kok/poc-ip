-- =============================================
-- Instant Payments PoC — Database Init Script
-- =============================================

-- Payments table (PK = transaction_id UUID)
CREATE TABLE IF NOT EXISTS payments (
    transaction_id  UUID            PRIMARY KEY,
    payer_name      VARCHAR(255)    NOT NULL,
    payer_bank      VARCHAR(255)    NOT NULL,
    payer_country   VARCHAR(3)      NOT NULL,
    payer_account   VARCHAR(50)     NOT NULL,
    payee_name      VARCHAR(255)    NOT NULL,
    payee_bank      VARCHAR(255)    NOT NULL,
    payee_country   VARCHAR(3)      NOT NULL,
    payee_account   VARCHAR(50)     NOT NULL,
    payment_instruction TEXT,
    execution_date  DATE            NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    fraud_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Audit logs table (CDC pattern)
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  UUID            NOT NULL,
    operation       VARCHAR(10)     NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    changed_fields  TEXT[],
    source_ts       TIMESTAMPTZ     NOT NULL,
    captured_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_transaction_id ON audit_logs(transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_operation ON audit_logs(operation);
CREATE INDEX IF NOT EXISTS idx_audit_source_ts ON audit_logs(source_ts);

-- Enable full replica identity for CDC (captures before state on UPDATE/DELETE)
ALTER TABLE payments REPLICA IDENTITY FULL;

-- Create publication for CDC (Debezium needs this)
CREATE PUBLICATION dbz_publication FOR TABLE payments;

-- Create CDC replication user with proper permissions
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'debezium') THEN
        CREATE ROLE debezium WITH REPLICATION LOGIN PASSWORD 'dbz_pass';
    END IF;
END $$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
GRANT USAGE ON SCHEMA public TO debezium;
ALTER PUBLICATION dbz_publication OWNER TO debezium;
