package com.example.rental_management.billing.dto;

import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe read-only view of a LedgerEntry.
 */
public record LedgerEntryResponse(
        Long id,
        Long leaseId,
        LedgerEntryType entryType,
        BigDecimal amount,
        String description,
        Long rentChargeId,
        Long paymentId,
        LocalDateTime createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry e) {
        return new LedgerEntryResponse(
                e.getId(),
                e.getLease().getId(),
                e.getEntryType(),
                e.getAmount(),
                e.getDescription(),
                e.getRentChargeId(),
                e.getPaymentId(),
                e.getCreatedAt()
        );
    }
}
