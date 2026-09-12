ALTER TABLE leases
ALTER COLUMN monthly_rent TYPE NUMERIC(12,2)
USING monthly_rent::NUMERIC(12,2);

ALTER TABLE rent_payments
ALTER COLUMN amount_due TYPE NUMERIC(12,2)
USING amount_due::NUMERIC(12,2);

ALTER TABLE rent_payments
ALTER COLUMN amount_paid TYPE NUMERIC(12,2)
USING amount_paid::NUMERIC(12,2);

ALTER TABLE rent_payments
ALTER COLUMN late_fee TYPE NUMERIC(12,2)
USING late_fee::NUMERIC(12,2);