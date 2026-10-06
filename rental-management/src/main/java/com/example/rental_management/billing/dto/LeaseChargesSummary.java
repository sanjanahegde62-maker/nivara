package com.example.rental_management.billing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Summary of all charges for a lease plus the calculated balance.
 */
public record LeaseChargesSummary(
        Long leaseId,
        BigDecimal totalCharged,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,
        List<RentChargeResponse> charges
) {}
