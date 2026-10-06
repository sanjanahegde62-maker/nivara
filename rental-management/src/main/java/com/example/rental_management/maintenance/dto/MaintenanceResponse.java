package com.example.rental_management.maintenance.dto;

import java.time.LocalDateTime;

/**
 * Safe read-only view of a MaintenanceRequest. Never exposes JPA entities.
 */
public record MaintenanceResponse(
        Long id,
        Long unitId,
        Long tenantId,
        Long assignedStaffId,
        String description,
        String priority,
        String status,
        LocalDateTime dueAt,
        boolean slaBreached,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
