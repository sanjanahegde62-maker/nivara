package com.example.rental_management.application.dto;

import com.example.rental_management.application.entity.Application;

import java.time.LocalDateTime;

/**
 * Safe read-only view of an Application.
 * Never exposes User, Unit, Property entities or passwordHash.
 */
public record ApplicationResponse(
        Long id,
        Long unitId,
        String unitNumber,
        Long tenantId,
        String tenantName,
        String status,
        LocalDateTime appliedAt,
        LocalDateTime reviewedAt
) {
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getUnit().getId(),
                a.getUnit().getUnitNumber(),
                a.getTenant().getId(),
                a.getTenant().getName(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getReviewedAt()
        );
    }
}
