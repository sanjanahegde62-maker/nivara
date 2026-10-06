package com.example.rental_management.lease.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.lease.dto.LeaseResponse;
import com.example.rental_management.lease.service.LeaseService;
import com.example.rental_management.user.entity.Role;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    /**
     * Create a lease from an approved application.
     * Only OWNER or MANAGER should call this.
     */
    @PostMapping("/applications/{applicationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseResponse createLease(
            @PathVariable Long applicationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return LeaseResponse.from(leaseService.createLease(applicationId, startDate, endDate));
    }

    /**
     * GET /leases — role-aware list:
     *   TENANT   → returns their own active leases
     *   OWNER    → returns leases for properties they own
     *   MANAGER  → returns leases for properties assigned to them (re-uses owner-style query via service)
     *   Others   → 403 thrown by service
     */
    @GetMapping
    public List<LeaseResponse> getLeases() {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return leaseService.getLeasesForCurrentUser(userId, role)
                .stream()
                .map(LeaseResponse::from)
                .toList();
    }

    /**
     * GET /leases/my — tenant shortcut (backward-compat, kept for existing callers).
     */
    @GetMapping("/my")
    public List<LeaseResponse> getMyLeases() {
        Long tenantId = SecurityUtil.getCurrentUserId();
        return leaseService.getMyLeases(tenantId)
                .stream()
                .map(LeaseResponse::from)
                .toList();
    }

    /**
     * GET /leases/{leaseId} — get a single lease by ID.
     * Row-level check is in the service.
     */
    @GetMapping("/{leaseId}")
    public LeaseResponse getLeaseById(@PathVariable Long leaseId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return LeaseResponse.from(leaseService.getLeaseById(leaseId, userId, role));
    }

    /**
     * GET /leases/property — kept for backward compatibility with existing OWNER usage.
     */
    @GetMapping("/property")
    public List<LeaseResponse> getPropertyLeases() {
        Long ownerId = SecurityUtil.getCurrentUserId();
        return leaseService.getPropertyLeases(ownerId)
                .stream()
                .map(LeaseResponse::from)
                .toList();
    }
}
