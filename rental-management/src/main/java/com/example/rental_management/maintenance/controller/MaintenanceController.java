package com.example.rental_management.maintenance.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.maintenance.dto.MaintenanceResponse;
import com.example.rental_management.maintenance.service.MaintenanceRequestService;
import com.example.rental_management.user.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance-requests")
public class MaintenanceController {

    private final MaintenanceRequestService maintenanceRequestService;

    public MaintenanceController(MaintenanceRequestService maintenanceRequestService) {
        this.maintenanceRequestService = maintenanceRequestService;
    }

    /** Create a maintenance request for a unit (TENANT only). */
    @PostMapping("/units/{unitId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResponse createRequest(
            @PathVariable Long unitId,
            @RequestParam String description,
            @RequestParam String priority) {
        Long tenantId = SecurityUtil.getCurrentUserId();
        return maintenanceRequestService.createRequest(unitId, tenantId, description, priority);
    }

    /**
     * List all maintenance requests visible to the current user.
     * TENANT: their own; OWNER: their properties; MANAGER: assigned properties;
     * MAINTENANCE_STAFF: requests assigned to them.
     */
    @GetMapping
    public List<MaintenanceResponse> getRequests() {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return maintenanceRequestService.getRequests(userId, role);
    }

    /** Get a single maintenance request by ID (row-level authorised). */
    @GetMapping("/{id}")
    public MaintenanceResponse getRequest(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return maintenanceRequestService.getRequest(id, userId, role);
    }

    /** Assign maintenance staff to a request (OWNER or MANAGER only). */
    @PatchMapping("/{id}/assign")
    public MaintenanceResponse assignRequest(
            @PathVariable Long id, @RequestParam Long staffId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return maintenanceRequestService.assignRequest(id, staffId, userId, role);
    }

    /** Advance the status of a request through the defined lifecycle. */
    @PatchMapping("/{id}/status")
    public MaintenanceResponse updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        return maintenanceRequestService.updateStatus(id, status, userId, role);
    }
}
