CREATE TABLE properties (
                            id BIGSERIAL PRIMARY KEY,

                            owner_id BIGINT NOT NULL,

                            name VARCHAR(255) NOT NULL,

                            address VARCHAR(255) NOT NULL,

                            property_type VARCHAR(100) NOT NULL,

                            description TEXT,

                            created_at TIMESTAMP NOT NULL,

                            updated_at TIMESTAMP NOT NULL,

                            CONSTRAINT fk_properties_owner
                                FOREIGN KEY (owner_id)
                                    REFERENCES users(id)
);