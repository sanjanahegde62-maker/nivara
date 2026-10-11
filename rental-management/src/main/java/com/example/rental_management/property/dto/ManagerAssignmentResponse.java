package com.example.rental_management.property.dto;

import com.example.rental_management.property.entity.PropertyManager;

import java.time.LocalDateTime;

/**
 * Safe view of a property–manager assignment.
 * Never exposes the User entity or passwordHash.
 */
public record ManagerAssignmentResponse(
        Long managerId,
        String managerName,
        String managerEmail,
        LocalDateTime assignedAt
) {
    public static ManagerAssignmentResponse from(PropertyManager pm) {
        return new ManagerAssignmentResponse(
                pm.getManager().getId(),
                pm.getManager().getName(),
                pm.getManager().getEmail(),
                pm.getAssignedAt()
        );
    }
}
