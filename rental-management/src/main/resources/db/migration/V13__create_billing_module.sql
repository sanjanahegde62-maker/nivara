-- V13: Billing module — rent_charges, billing_payments, ledger_entries
-- These tables are entirely new; the existing rent_payments table is NOT dropped
-- so existing data and the old billing service remain unaffected.

-- ─── rent_charges ────────────────────────────────────────────────────────────
-- One row per lease per billing period.
-- billing_period is stored as the first day of the month (e.g. 2024-06-01).
-- The unique constraint enforces idempotency at the DB level.
CREATE TABLE rent_charges (
    id              BIGSERIAL       PRIMARY KEY,
    lease_id        BIGINT          NOT NULL,
    billing_period  DATE            NOT NULL,
    amount          NUMERIC(12, 2)  NOT NULL,
    due_date        DATE            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    CONSTRAINT fk_rent_charge_lease
        FOREIGN KEY (lease_id) REFERENCES leases(id),

    CONSTRAINT uq_rent_charge_lease_period
        UNIQUE (lease_id, billing_period),

    CONSTRAINT chk_rent_charge_status
        CHECK (status IN ('PENDING', 'OVERDUE', 'PAID')),

    CONSTRAINT chk_rent_charge_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_rent_charge_lease_id     ON rent_charges (lease_id);
CREATE INDEX idx_rent_charge_due_date     ON rent_charges (due_date);
CREATE INDEX idx_rent_charge_status       ON rent_charges (status);

-- ─── billing_payments ────────────────────────────────────────────────────────
-- One row per payment transaction against a rent charge.
-- Named "billing_payments" to avoid collision with the legacy rent_payments table.
CREATE TABLE billing_payments (
    id              BIGSERIAL       PRIMARY KEY,
    rent_charge_id  BIGINT          NOT NULL,
    amount          NUMERIC(12, 2)  NOT NULL,
    payment_date    DATE            NOT NULL,
    payment_method  VARCHAR(40)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    reference       VARCHAR(120),
    created_at      TIMESTAMP       NOT NULL,

    CONSTRAINT fk_billing_payment_charge
        FOREIGN KEY (rent_charge_id) REFERENCES rent_charges(id),

    CONSTRAINT chk_billing_payment_status
        CHECK (status IN ('COMPLETED', 'REFUNDED')),

    CONSTRAINT chk_billing_payment_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_billing_payment_charge_id ON billing_payments (rent_charge_id);
CREATE INDEX idx_billing_payment_date       ON billing_payments (payment_date);

-- ─── ledger_entries ──────────────────────────────────────────────────────────
-- Immutable audit trail of all financial movements per lease.
-- Rows are NEVER updated or deleted — corrections are new entries.
CREATE TABLE ledger_entries (
    id              BIGSERIAL       PRIMARY KEY,
    lease_id        BIGINT          NOT NULL,
    entry_type      VARCHAR(20)     NOT NULL,
    amount          NUMERIC(12, 2)  NOT NULL,
    description     VARCHAR(300)    NOT NULL,
    rent_charge_id  BIGINT,
    payment_id      BIGINT,
    created_at      TIMESTAMP       NOT NULL,

    CONSTRAINT fk_ledger_lease
        FOREIGN KEY (lease_id) REFERENCES leases(id),

    CONSTRAINT fk_ledger_rent_charge
        FOREIGN KEY (rent_charge_id) REFERENCES rent_charges(id),

    CONSTRAINT fk_ledger_payment
        FOREIGN KEY (payment_id) REFERENCES billing_payments(id),

    CONSTRAINT chk_ledger_entry_type
        CHECK (entry_type IN ('RENT_CHARGE', 'PAYMENT', 'LATE_FEE')),

    CONSTRAINT chk_ledger_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_ledger_lease_id       ON ledger_entries (lease_id);
CREATE INDEX idx_ledger_rent_charge_id ON ledger_entries (rent_charge_id);
CREATE INDEX idx_ledger_payment_id     ON ledger_entries (payment_id);
CREATE INDEX idx_ledger_created_at     ON ledger_entries (created_at);
