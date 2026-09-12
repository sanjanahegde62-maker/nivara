CREATE TABLE applications (
                              id BIGSERIAL PRIMARY KEY,

                              unit_id BIGINT NOT NULL,
                              tenant_id BIGINT NOT NULL,

                              status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                              applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              reviewed_at TIMESTAMP,

                              CONSTRAINT fk_applications_unit
                                  FOREIGN KEY (unit_id)
                                      REFERENCES units(id),

                              CONSTRAINT fk_applications_tenant
                                  FOREIGN KEY (tenant_id)
                                      REFERENCES users(id)
);