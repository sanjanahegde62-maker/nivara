package com.example.rental_management.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-property expense summary.
 *
 * Expenses = sum of all LATE_FEE ledger entries for leases
 * belonging to the property, optionally filtered by date window.
 * Late fees are the only expense-type entries in the current data model.
 */
public record ExpenseReportRow(
        Long propertyId,
        String propertyName,
        String propertyAddress,
        LocalDate from,
        LocalDate to,
        BigDecimal totalLateFees,
        long lateFeeCount
) {}
