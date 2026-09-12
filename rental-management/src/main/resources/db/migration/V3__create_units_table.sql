CREATE TABLE units (
                       id BIGSERIAL PRIMARY KEY,

                       property_id BIGINT NOT NULL,

                       unit_number VARCHAR(100) NOT NULL,

                       unit_type VARCHAR(100) NOT NULL,

                       monthly_rent DOUBLE PRECISION NOT NULL,

                       status VARCHAR(50) NOT NULL,

                       created_at TIMESTAMP NOT NULL,

                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT fk_units_property
                           FOREIGN KEY (property_id)
                               REFERENCES properties(id)
);