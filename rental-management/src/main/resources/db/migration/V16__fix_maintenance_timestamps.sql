-- V16: Convert maintenance_requests.created_at and updated_at from DATE to TIMESTAMP
--
-- V12 created these columns as DATE, losing time precision.
-- The entity now uses LocalDateTime which maps to TIMESTAMP.
-- This migration casts the existing DATE values to TIMESTAMP WITH TIME ZONE cast to TIMESTAMP.

ALTER TABLE maintenance_requests
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::TIMESTAMP,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::TIMESTAMP;
