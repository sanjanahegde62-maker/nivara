package com.example.rental_management.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-property income summary.
 *
 * Income = sum of all COMPLETED billing_payments for leases
 * belonging to the property, optionally filtered by date window.
 */
public record IncomeReportRow(
        Long propertyId,
        String propertyName,
        String propertyAddress,
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        long paymentCount
) {}
