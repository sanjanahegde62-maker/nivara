CREATE TABLE rent_payments (
                               id BIGSERIAL PRIMARY KEY,

                               lease_id BIGINT NOT NULL,

                               due_date DATE NOT NULL,

                               paid_date DATE,

                               amount_due DOUBLE PRECISION NOT NULL,

                               amount_paid DOUBLE PRECISION,

                               status VARCHAR(20) NOT NULL,

                               created_at TIMESTAMP NOT NULL,

                               updated_at TIMESTAMP NOT NULL,

                               CONSTRAINT fk_rent_payment_lease
                                   FOREIGN KEY (lease_id)
                                       REFERENCES leases(id)
);