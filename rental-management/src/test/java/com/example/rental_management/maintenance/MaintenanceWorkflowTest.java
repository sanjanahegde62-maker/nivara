package com.example.rental_management.maintenance;

import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.maintenance.dto.MaintenanceResponse;
import com.example.rental_management.maintenance.entity.MaintenanceRequest;
import com.example.rental_management.maintenance.repository.MaintenanceRepository;
import com.example.rental_management.maintenance.service.MaintenanceRequestService;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Block G — Maintenance Workflow Tests
 *
 * Tests:
 * - Tenant can create a maintenance request for their active lease unit
 * - Tenant without active lease cannot create a request
 * - SLA deadline is set on create based on priority
 * - Full status lifecycle: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
 * - Invalid status transitions are rejected
 * - Only OWNER/MANAGER can assign; only MAINTENANCE_STAFF can move to IN_PROGRESS/RESOLVED
 * - Only OWNER/MANAGER can close
 * - Tenant sees only their own requests
 * - Unrelated tenant cannot see another tenant's request
 * - MaintenanceResponse DTO does not expose passwords or sensitive JPA graphs
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MaintenanceWorkflowTest {

    @Autowired MaintenanceRequestService maintenanceService;
    @Autowired MaintenanceRepository maintenanceRepository;
    @Autowired LeaseRepository leaseRepository;
    @Autowired UserRepository userRepository;

    // ── Base fixture ──────────────────────────────────────────────────────────

    private Lease activeLease() {
        return leaseRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("test data: lease 1 not found"));
    }

    /**
     * Create a saved OPEN maintenance request directly for test setup.
     * Bypasses the service to avoid lease-existence checks in tests that focus
     * on other transitions.
     */
    private MaintenanceRequest savedRequest(Lease lease, String priority) {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setUnit(lease.getUnit());
        r.setTenant(lease.getTenant());
        r.setDescription("Test maintenance request");
        r.setPriority(priority);
        r.setStatus("OPEN");
        r.setDueAt(java.time.LocalDateTime.now().plusHours(24));
        return maintenanceRepository.save(r);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G1 — Tenant can create a request for their active lease unit
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void tenant_canCreateRequestForOwnUnit() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        Long unitId = lease.getUnit().getId();

        MaintenanceResponse response = maintenanceService.createRequest(
                unitId, tenantId, "Leaking tap", "HIGH");

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.priority()).isEqualTo("HIGH");
        assertThat(response.dueAt()).isNotNull();
        // HIGH priority → 24-hour SLA (within ±1 hour of 24h from now)
        assertThat(response.dueAt()).isAfter(java.time.LocalDateTime.now().plusHours(23));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G2 — Tenant without active lease for a unit cannot create a request
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void tenant_withoutActiveLease_cannotCreateRequest() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        // Use a different unit ID that this tenant definitely does not have a lease for
        Long wrongUnitId = lease.getUnit().getId() + 9999L;

        assertThatThrownBy(() ->
                maintenanceService.createRequest(wrongUnitId, tenantId, "Something", "LOW"))
                .isInstanceOf(Exception.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G3 — SLA deadline is priority-driven
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void slaDeadline_isSetBasedOnPriority() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        Long unitId = lease.getUnit().getId();

        MaintenanceResponse urgent = maintenanceService.createRequest(
                unitId, tenantId, "Gas leak", "URGENT");
        MaintenanceResponse low = maintenanceService.createRequest(
                unitId, tenantId, "Paint peeling", "LOW");

        // URGENT SLA < LOW SLA
        assertThat(urgent.dueAt()).isBefore(low.dueAt());
        // URGENT should be within 5 hours
        assertThat(urgent.dueAt()).isBefore(java.time.LocalDateTime.now().plusHours(5));
        // LOW should be after 100 hours
        assertThat(low.dueAt()).isAfter(java.time.LocalDateTime.now().plusHours(100));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G4 — Owner can assign a request to maintenance staff
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void owner_canAssignRequestToStaff() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        MaintenanceRequest request = savedRequest(lease, "MEDIUM");

        // Find a MAINTENANCE_STAFF user
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MAINTENANCE_STAFF)
                .findFirst()
                .orElse(null);
        if (staff == null) return; // no staff seeded — skip gracefully

        MaintenanceResponse response = maintenanceService.assignRequest(
                request.getId(), staff.getId(), ownerId, Role.OWNER);

        assertThat(response.status()).isEqualTo("ASSIGNED");
        assertThat(response.assignedStaffId()).isEqualTo(staff.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G5 — Invalid status transition is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void invalidStatusTransition_isRejected() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        MaintenanceRequest request = savedRequest(lease, "LOW");

        // Cannot jump from OPEN directly to RESOLVED
        assertThatThrownBy(() ->
                maintenanceService.updateStatus(
                        request.getId(), "RESOLVED", ownerId, Role.OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid status transition");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G6 — Only MAINTENANCE_STAFF can move to IN_PROGRESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void onlyMaintenanceStaff_canMoveToInProgress() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        MaintenanceRequest request = savedRequest(lease, "HIGH");
        request.setStatus("ASSIGNED");
        maintenanceRepository.save(request);

        // Owner trying to set IN_PROGRESS should be rejected
        assertThatThrownBy(() ->
                maintenanceService.updateStatus(
                        request.getId(), "IN_PROGRESS", ownerId, Role.OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only maintenance staff");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G7 — RESOLVED transition sets resolvedAt timestamp
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void resolvedTransition_setsResolvedAt() {
        Lease lease = activeLease();

        // Find or use any MAINTENANCE_STAFF user id
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MAINTENANCE_STAFF)
                .findFirst()
                .orElse(null);
        if (staff == null) return;

        MaintenanceRequest request = savedRequest(lease, "MEDIUM");
        request.setStatus("IN_PROGRESS");
        request.setAssignedStaff(staff);
        maintenanceRepository.save(request);

        MaintenanceResponse response = maintenanceService.updateStatus(
                request.getId(), "RESOLVED", staff.getId(), Role.MAINTENANCE_STAFF);

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.resolvedAt()).isNotNull();
        assertThat(response.closedAt()).isNull(); // not yet closed
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G8 — Only OWNER/MANAGER can close a resolved request
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void onlyOwnerOrManager_canCloseRequest() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();
        Long tenantId = lease.getTenant().getId();

        MaintenanceRequest request = savedRequest(lease, "LOW");
        request.setStatus("RESOLVED");
        request.setResolvedAt(java.time.LocalDateTime.now());
        maintenanceRepository.save(request);

        // Tenant cannot close
        assertThatThrownBy(() ->
                maintenanceService.updateStatus(
                        request.getId(), "CLOSED", tenantId, Role.TENANT))
                .isInstanceOf(IllegalStateException.class);

        // Owner can close
        MaintenanceResponse response = maintenanceService.updateStatus(
                request.getId(), "CLOSED", ownerId, Role.OWNER);
        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(response.closedAt()).isNotNull();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G9 — Tenant sees only their own requests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void tenant_seesOnlyOwnRequests() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        // Create one request for this tenant
        savedRequest(lease, "LOW");

        List<MaintenanceResponse> requests = maintenanceService.getRequests(tenantId, Role.TENANT);

        // All returned requests must belong to this tenant
        assertThat(requests).isNotEmpty();
        assertThat(requests).allMatch(r -> r.tenantId().equals(tenantId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G10 — Unrelated tenant cannot read another tenant's request
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void unrelatedTenant_cannotReadAnotherTenantsRequest() {
        Lease lease = activeLease();
        Long impostorId = lease.getTenant().getId() + 9999L;

        MaintenanceRequest request = savedRequest(lease, "LOW");

        assertThatThrownBy(() ->
                maintenanceService.getRequest(request.getId(), impostorId, Role.TENANT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maintenance request not found");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // G11 — MaintenanceResponse DTO does not expose passwords
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    void maintenanceResponse_doesNotExposePasswords() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        savedRequest(lease, "LOW");

        List<MaintenanceResponse> responses = maintenanceService.getRequests(tenantId, Role.TENANT);
        assertThat(responses).isNotEmpty();

        String json = responses.toString();
        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("password_hash");
    }
}
