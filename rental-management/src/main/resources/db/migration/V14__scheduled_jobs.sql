-- V14: Scheduled Jobs support
--
-- 1. job_execution_log — one row per (job_name, run_date).
--    A UNIQUE constraint on (job_name, run_date) makes each job idempotent at the
--    database level: the first run inserts a row; a second run on the same date
--    hits a unique-key violation which the application catches and skips.
--
-- 2. Unique constraint on ledger_entries for late-fee entries:
--    (rent_charge_id, entry_type, fee_date) where fee_date is the calendar date
--    the fee was assessed.  We add a fee_date column (nullable — only populated
--    for LATE_FEE entries) and a partial unique index.
--
-- 3. Lease status value EXPIRED is already handled by the CHECK in leases
--    (currently none) — we add it now.  The check on status is VARCHAR so
--    we just update the application enum; no DDL change needed for leases.status.

-- ─── job_execution_log ────────────────────────────────────────────────────────
CREATE TABLE job_execution_log (
    id          BIGSERIAL   PRIMARY KEY,
    job_name    VARCHAR(80) NOT NULL,
    run_date    DATE        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    rows_affected INT       NOT NULL DEFAULT 0,
    started_at  TIMESTAMP   NOT NULL,
    finished_at TIMESTAMP,

    CONSTRAINT uq_job_run_date UNIQUE (job_name, run_date),
    CONSTRAINT chk_job_status  CHECK  (status IN ('SUCCESS','FAILED','SKIPPED'))
);

CREATE INDEX idx_job_log_job_name ON job_execution_log (job_name);
CREATE INDEX idx_job_log_run_date ON job_execution_log (run_date);

-- ─── fee_date column on ledger_entries ────────────────────────────────────────
-- Populated only for LATE_FEE entries; NULL for RENT_CHARGE and PAYMENT.
ALTER TABLE ledger_entries
    ADD COLUMN fee_date DATE;

-- Partial unique index: for a given charge, only one LATE_FEE entry per calendar
-- day.  This is the database-level idempotency guard for the LateFeeJob.
CREATE UNIQUE INDEX uq_late_fee_per_charge_per_day
    ON ledger_entries (rent_charge_id, fee_date)
    WHERE entry_type = 'LATE_FEE' AND fee_date IS NOT NULL;

-- ─── expiry_notice_date column on ledger_entries ──────────────────────────────
-- Populated only for LEASE_EXPIRY_NOTICE entries.
-- We extend the entry_type CHECK to include the new value.
ALTER TABLE ledger_entries
    DROP CONSTRAINT chk_ledger_entry_type;

ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_ledger_entry_type
        CHECK (entry_type IN ('RENT_CHARGE', 'PAYMENT', 'LATE_FEE', 'LEASE_EXPIRY_NOTICE'));

-- Partial unique index: one expiry notice per lease per calendar day.
CREATE UNIQUE INDEX uq_expiry_notice_per_lease_per_day
    ON ledger_entries (lease_id, fee_date)
    WHERE entry_type = 'LEASE_EXPIRY_NOTICE' AND fee_date IS NOT NULL;
