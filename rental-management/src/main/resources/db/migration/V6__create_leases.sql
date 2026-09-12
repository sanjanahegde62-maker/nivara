CREATE TABLE leases (
                        id BIGSERIAL PRIMARY KEY,
                        application_id BIGINT NOT NULL UNIQUE,
                        tenant_id BIGINT NOT NULL,
                        unit_id BIGINT NOT NULL,
                        start_date DATE NOT NULL,
                        end_date DATE NOT NULL,
                        monthly_rent DOUBLE PRECISION NOT NULL,
                        status VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL,

                        CONSTRAINT fk_lease_application
                            FOREIGN KEY (application_id)
                                REFERENCES applications(id),

                        CONSTRAINT fk_lease_tenant
                            FOREIGN KEY (tenant_id)
                                REFERENCES users(id),

                        CONSTRAINT fk_lease_unit
                            FOREIGN KEY (unit_id)
                                REFERENCES units(id)
);