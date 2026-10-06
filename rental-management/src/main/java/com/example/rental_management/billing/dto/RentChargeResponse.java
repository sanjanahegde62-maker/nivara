package com.example.rental_management.billing.dto;

import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.RentCharge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Safe read-only view of a RentCharge. Never exposes JPA entities.
 */
public record RentChargeResponse(
        Long id,
        Long leaseId,
        YearMonth billingPeriod,
        BigDecimal amount,
        LocalDate dueDate,
        ChargeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RentChargeResponse from(RentCharge rc) {
        return new RentChargeResponse(
                rc.getId(),
                rc.getLease().getId(),
                rc.getBillingYearMonth(),
                rc.getAmount(),
                rc.getDueDate(),
                rc.getStatus(),
                rc.getCreatedAt(),
                rc.getUpdatedAt()
        );
    }
}
