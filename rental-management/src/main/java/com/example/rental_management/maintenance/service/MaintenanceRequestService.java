package com.example.rental_management.maintenance.service;

import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.maintenance.dto.MaintenanceResponse;
import com.example.rental_management.maintenance.entity.MaintenanceRequest;
import com.example.rental_management.maintenance.repository.MaintenanceRepository;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.UnitRepository;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRepository maintenanceRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;

    public MaintenanceRequestService(
            MaintenanceRepository maintenanceRepository,
            UnitRepository unitRepository,
            UserRepository userRepository,
            LeaseRepository leaseRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
    }

    /**
     * SLA deadlines by priority (hours from now).
     * URGENT: 4 h, HIGH: 24 h, MEDIUM: 72 h, LOW: 168 h (7 days).
     */
    private LocalDateTime slaDeadlineFor(String priority) {
        return switch (priority.toUpperCase()) {
            case "URGENT" -> LocalDateTime.now().plusHours(4);
            case "HIGH"   -> LocalDateTime.now().plusHours(24);
            case "MEDIUM" -> LocalDateTime.now().plusHours(72);
            default       -> LocalDateTime.now().plusHours(168); // LOW
        };
    }

    public MaintenanceResponse createRequest(
            Long unitId, Long tenantId, String description, String priority) {

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalStateException("unit not found"));

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("tenant not found"));

        if (tenant.getRole() != Role.TENANT) {
            throw new IllegalStateException("only tenants can create maintenance requests");
        }

        leaseRepository.findByTenantIdAndUnitIdAndStatus(tenantId, unitId, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException(
                        "tenant does not have an active lease for this unit"));

        String normalisedPriority = priority.toUpperCase();

        MaintenanceRequest request = new MaintenanceRequest();
        request.setUnit(unit);
        request.setTenant(tenant);
        request.setDescription(description);
        request.setPriority(normalisedPriority);
        request.setStatus("OPEN");
        request.setDueAt(slaDeadlineFor(normalisedPriority));

        return toResponse(maintenanceRepository.save(request));
    }

    public MaintenanceResponse getRequest(Long requestId, Long userId, Role role) {
        return toResponse(getAuthorizedRequest(requestId, userId, role));
    }

    public List<MaintenanceResponse> getRequests(Long userId, Role role) {
        List<MaintenanceRequest> requests = switch (role) {
            case TENANT           -> maintenanceRepository.findByTenantId(userId);
            case OWNER            -> maintenanceRepository.findByUnitPropertyOwnerId(userId);
            case MANAGER          -> maintenanceRepository.findByManagerId(userId);
            case MAINTENANCE_STAFF -> maintenanceRepository.findByAssignedStaffId(userId);
        };
        return requests.stream().map(this::toResponse).toList();
    }

    public MaintenanceResponse assignRequest(
            Long requestId, Long staffId, Long userId, Role role) {

        if (role != Role.OWNER && role != Role.MANAGER) {
            throw new IllegalStateException(
                    "only owner or manager can assign maintenance requests");
        }

        MaintenanceRequest request;
        if (role == Role.OWNER) {
            request = maintenanceRepository
                    .findByIdAndUnitPropertyOwnerId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
        } else {
            request = maintenanceRepository
                    .findByIdAndManagerId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
        }

        if (!"OPEN".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "can only assign a request that is currently OPEN");
        }

        User staff = userRepository.findByIdAndRole(staffId, Role.MAINTENANCE_STAFF)
                .orElseThrow(() -> new IllegalStateException("maintenance staff not found"));

        request.setAssignedStaff(staff);
        request.setStatus("ASSIGNED");

        return toResponse(maintenanceRepository.save(request));
    }

    public MaintenanceResponse updateStatus(
            Long requestId, String newStatus, Long userId, Role role) {

        MaintenanceRequest request = getAuthorizedRequest(requestId, userId, role);

        String currentStatus = request.getStatus();
        boolean validTransition = switch (currentStatus) {
            case "OPEN"        -> "ASSIGNED".equals(newStatus);
            case "ASSIGNED"    -> "IN_PROGRESS".equals(newStatus);
            case "IN_PROGRESS" -> "RESOLVED".equals(newStatus);
            case "RESOLVED"    -> "CLOSED".equals(newStatus);
            default            -> false; // CLOSED is terminal
        };

        if (!validTransition) {
            throw new IllegalStateException(
                    "invalid status transition from " + currentStatus + " to " + newStatus);
        }

        if ("IN_PROGRESS".equals(newStatus) || "RESOLVED".equals(newStatus)) {
            if (role != Role.MAINTENANCE_STAFF) {
                throw new IllegalStateException(
                        "only maintenance staff can update this status");
            }
        }
        if ("CLOSED".equals(newStatus)) {
            if (role != Role.OWNER && role != Role.MANAGER) {
                throw new IllegalStateException(
                        "only owner or manager can close a request");
            }
        }

        request.setStatus(newStatus);

        // Audit timestamps for terminal/significant transitions
        if ("RESOLVED".equals(newStatus)) {
            request.setResolvedAt(LocalDateTime.now());
        }
        if ("CLOSED".equals(newStatus)) {
            request.setClosedAt(LocalDateTime.now());
        }

        return toResponse(maintenanceRepository.save(request));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MaintenanceResponse toResponse(MaintenanceRequest r) {
        return new MaintenanceResponse(
                r.getId(),
                r.getUnit().getId(),
                r.getTenant().getId(),
                r.getAssignedStaff() != null ? r.getAssignedStaff().getId() : null,
                r.getDescription(),
                r.getPriority(),
                r.getStatus(),
                r.getDueAt(),
                r.isSlaBreached(),
                r.getResolvedAt(),
                r.getClosedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private MaintenanceRequest getAuthorizedRequest(
            Long requestId, Long userId, Role role) {
        return switch (role) {
            case TENANT -> maintenanceRepository
                    .findByIdAndTenantId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
            case OWNER -> maintenanceRepository
                    .findByIdAndUnitPropertyOwnerId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
            case MANAGER -> maintenanceRepository
                    .findByIdAndManagerId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
            case MAINTENANCE_STAFF -> maintenanceRepository
                    .findByIdAndAssignedStaffId(requestId, userId)
                    .orElseThrow(() -> new IllegalStateException("maintenance request not found"));
        };
    }
}
