CREATE TABLE property_managers (
                                   property_id BIGINT NOT NULL,
                                   manager_id BIGINT NOT NULL,
                                   assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT pk_property_managers
                                       PRIMARY KEY (property_id, manager_id),

                                   CONSTRAINT fk_property_managers_property
                                       FOREIGN KEY (property_id)
                                           REFERENCES properties(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_property_managers_manager
                                       FOREIGN KEY (manager_id)
                                           REFERENCES users(id)
);