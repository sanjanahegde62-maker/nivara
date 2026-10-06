CREATE TABLE maintenance_requests (
                                      id BIGSERIAL PRIMARY KEY,

                                      unit_id BIGINT NOT NULL,
                                      tenant_id BIGINT NOT NULL,
                                      assigned_staff_id BIGINT,

                                      description VARCHAR(1000) NOT NULL,
                                      status VARCHAR(30) NOT NULL,
                                      priority VARCHAR(20) NOT NULL,

                                      created_at DATE NOT NULL,
                                      updated_at DATE NOT NULL,

                                      CONSTRAINT fk_maintenance_unit
                                          FOREIGN KEY (unit_id)
                                              REFERENCES units(id),

                                      CONSTRAINT fk_maintenance_tenant
                                          FOREIGN KEY (tenant_id)
                                              REFERENCES users(id),

                                      CONSTRAINT fk_maintenance_staff
                                          FOREIGN KEY (assigned_staff_id)
                                              REFERENCES users(id)
);