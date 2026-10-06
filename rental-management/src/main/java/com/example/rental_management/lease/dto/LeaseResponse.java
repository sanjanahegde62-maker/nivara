package com.example.rental_management.lease.dto;

import com.example.rental_management.lease.entity.Lease;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe read-only projection of a Lease.
 * Never exposes JPA entities, Hibernate proxies, or sensitive User fields.
 */
public record LeaseResponse(
        Long id,
        Long tenantId,
        String tenantName,
        Long unitId,
        String unitNumber,
        Long propertyId,
        String propertyName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LeaseResponse from(Lease lease) {
        return new LeaseResponse(
                lease.getId(),
                lease.getTenant().getId(),
                lease.getTenant().getName(),
                lease.getUnit().getId(),
                lease.getUnit().getUnitNumber(),
                lease.getUnit().getProperty().getId(),
                lease.getUnit().getProperty().getName(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getStatus(),
                lease.getCreatedAt(),
                lease.getUpdatedAt()
        );
    }
}
