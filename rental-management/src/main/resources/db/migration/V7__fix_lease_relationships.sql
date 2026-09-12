ALTER TABLE leases
DROP CONSTRAINT fk_lease_tenant;

ALTER TABLE leases
    ADD CONSTRAINT fk_lease_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES users(id);