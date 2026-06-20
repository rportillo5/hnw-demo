-- =============================================================================
-- Day 5 schema addition — explain_audit_log table
-- Run this against your local PostgreSQL (taxdemo database).
--
-- Usage:
--   psql -h localhost -U taxdemo -d taxdemo -f day5_schema.sql
-- =============================================================================

CREATE TABLE IF NOT EXISTS explain_audit_log (
    id            UUID PRIMARY KEY,
    lot_id        UUID NOT NULL,
    prompt_hash   VARCHAR(64) NOT NULL,
    response_text TEXT NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_explain_audit_log_lot_id ON explain_audit_log(lot_id);
CREATE INDEX IF NOT EXISTS idx_explain_audit_log_created_at ON explain_audit_log(created_at);

-- Verification
SELECT COUNT(*) AS audit_log_row_count FROM explain_audit_log;
