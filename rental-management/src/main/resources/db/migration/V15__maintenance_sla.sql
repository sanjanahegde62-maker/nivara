-- V15: Maintenance module hardening — SLA, indexes, and CHECK constraints
--
-- Changes:
-- 1. Add due_at (SLA deadline) to maintenance_requests
-- 2. Add sla_breached flag (boolean, updated by SlaBreachJob)
-- 3. Add resolved_at and closed_at timestamps for audit
-- 4. Add CHECK constraints on status and priority
-- 5. Add performance indexes

-- ─── SLA columns ──────────────────────────────────────────────────────────────
ALTER TABLE maintenance_requests
    ADD COLUMN due_at        TIMESTAMP,
    ADD COLUMN sla_breached  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resolved_at   TIMESTAMP,
    ADD COLUMN closed_at     TIMESTAMP;

-- ─── CHECK constraints ────────────────────────────────────────────────────────
ALTER TABLE maintenance_requests
    ADD CONSTRAINT chk_maintenance_status
        CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED')),

    ADD CONSTRAINT chk_maintenance_priority
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT'));

-- ─── Performance indexes ──────────────────────────────────────────────────────
CREATE INDEX idx_maintenance_tenant_id     ON maintenance_requests (tenant_id);
CREATE INDEX idx_maintenance_unit_id       ON maintenance_requests (unit_id);
CREATE INDEX idx_maintenance_staff_id      ON maintenance_requests (assigned_staff_id);
CREATE INDEX idx_maintenance_status        ON maintenance_requests (status);
CREATE INDEX idx_maintenance_priority      ON maintenance_requests (priority);
CREATE INDEX idx_maintenance_due_at        ON maintenance_requests (due_at);
CREATE INDEX idx_maintenance_sla_breached  ON maintenance_requests (sla_breached);
