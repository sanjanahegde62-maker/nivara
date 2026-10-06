package com.example.rental_management.lease;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.lease.dto.LeaseResponse;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.lease.service.LeaseService;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Block D — Lease Workflow Tests
 *
 * Tests:
 * - Lease can only be created from an APPROVED application
 * - Duplicate lease for the same application is rejected
 * - TENANT sees only their own leases
 * - Unrelated TENANT cannot see another tenant's lease
 * - OWNER sees leases for their own properties only
 * - MAINTENANCE_STAFF cannot access leases
 * - LeaseResponse DTO does not expose passwords or JPA entity references
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeaseWorkflowTest {

    @Autowired LeaseService leaseService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;

    // ── Base fixture ──────────────────────────────────────────────────────────

    private Lease activeLease() {
        return leaseRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("test data: lease 1 not found"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D1 — Lease can only be created from an APPROVED application
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void leaseCreation_requiresApprovedApplication() {
        Lease lease = activeLease();
        Unit unit = lease.getUnit();
        User tenant = lease.getTenant();

        // Find another tenant to avoid duplicate app constraint
        User anotherTenant = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TENANT && !u.getId().equals(tenant.getId()))
                .findFirst()
                .orElse(null);
        if (anotherTenant == null) return;

        boolean alreadyApplied = applicationRepository.existsByUnitIdAndTenantId(
                unit.getId(), anotherTenant.getId());
        if (alreadyApplied) return;

        // Create a PENDING application
        Application pendingApp = new Application();
        pendingApp.setUnit(unit);
        pendingApp.setTenant(anotherTenant);
        pendingApp.setStatus("PENDING");
        pendingApp = applicationRepository.save(pendingApp);
        Long appId = pendingApp.getId();

        assertThatThrownBy(() ->
                leaseService.createLease(appId, LocalDate.now(), LocalDate.now().plusYears(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D2 — Duplicate lease for same application is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void duplicateLease_forSameApplication_isRejected() {
        Lease existing = activeLease();
        // The existing lease is already linked to an application
        Long appId = existing.getApplication().getId();

        assertThatThrownBy(() ->
                leaseService.createLease(appId, LocalDate.now(), LocalDate.now().plusYears(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D3 — TENANT sees only their own leases
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void tenant_seesOnlyOwnLeases() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        List<Lease> leases = leaseService.getLeasesForCurrentUser(tenantId, Role.TENANT);

        assertThat(leases).isNotEmpty();
        assertThat(leases).allMatch(l -> l.getTenant().getId().equals(tenantId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D4 — Unrelated TENANT cannot read another tenant's lease
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void unrelatedTenant_cannotReadAnotherTenantsLease() {
        Lease lease = activeLease();
        Long realTenantId = lease.getTenant().getId();
        Long impostorId = realTenantId + 9999L;

        assertThatThrownBy(() ->
                leaseService.getLeaseById(lease.getId(), impostorId, Role.TENANT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D5 — OWNER sees leases for their own properties
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void owner_seesLeasesForOwnProperties() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        List<Lease> leases = leaseService.getLeasesForCurrentUser(ownerId, Role.OWNER);

        assertThat(leases).isNotEmpty();
        // All returned leases should belong to this owner's properties
        assertThat(leases).allMatch(l ->
                l.getUnit().getProperty().getOwner().getId().equals(ownerId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D6 — Unrelated OWNER cannot read another owner's lease
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void unrelatedOwner_cannotReadAnotherOwnersLease() {
        Lease lease = activeLease();
        Long realOwnerId = lease.getUnit().getProperty().getOwner().getId();
        Long unrelatedOwnerId = realOwnerId + 9999L;

        assertThatThrownBy(() ->
                leaseService.getLeaseById(lease.getId(), unrelatedOwnerId, Role.OWNER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D7 — MAINTENANCE_STAFF cannot access leases
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void maintenanceStaff_cannotAccessLeases() {
        assertThatThrownBy(() ->
                leaseService.getLeasesForCurrentUser(99L, Role.MAINTENANCE_STAFF))
                .isInstanceOf(RuntimeException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D8 — LeaseResponse record does not expose sensitive fields
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void leaseResponse_doesNotExposeSensitiveFields() {
        Lease lease = activeLease();

        LeaseResponse response = LeaseResponse.from(lease);

        // Safe fields present
        assertThat(response.id()).isNotNull();
        assertThat(response.tenantId()).isEqualTo(lease.getTenant().getId());
        assertThat(response.status()).isEqualTo(lease.getStatus());

        // String representation must not contain password-related content
        String json = response.toString();
        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("password_hash");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D9 — Lease list for tenant includes all their leases
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void getMyLeases_returnsAllTenantLeases() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        List<Lease> leases = leaseService.getMyLeases(tenantId);

        assertThat(leases).isNotNull();
        assertThat(leases).anyMatch(l -> l.getId().equals(lease.getId()));
    }
}
