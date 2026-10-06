package com.example.rental_management.report.service;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.billing.exception.BillingAccessDeniedException;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.report.dto.ExpenseReportRow;
import com.example.rental_management.report.dto.IncomeReportRow;
import com.example.rental_management.report.repository.ReportRepository;
import com.example.rental_management.user.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporting service for income and expense aggregation.
 *
 * All data comes from database-level aggregation queries in ReportRepository.
 * No financial values are calculated in Java — all SUMs and COUNTs are done in SQL.
 *
 * Authorization:
 *   OWNER   — sees only properties they own
 *   MANAGER — sees only properties assigned to them
 *   TENANT / MAINTENANCE_STAFF — access denied
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public ReportService(
            ReportRepository reportRepository,
            PropertyManagerRepository propertyManagerRepository) {
        this.reportRepository = reportRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    // ── INCOME ────────────────────────────────────────────────────────────────

    /**
     * Income report: total completed payments received per property.
     *
     * @param from  optional start date (inclusive), null = unbounded
     * @param to    optional end date (inclusive), null = unbounded
     */
    @Transactional(readOnly = true)
    public List<IncomeReportRow> getIncomeReport(LocalDate from, LocalDate to) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role   = SecurityUtil.getCurrentUserRole();

        // Convert to String so PostgreSQL can CAST(? AS date) and handle NULL correctly
        String fromStr = from != null ? from.toString() : null;
        String toStr   = to   != null ? to.toString()   : null;

        List<Object[]> rows = switch (role) {
            case OWNER   -> reportRepository.incomeByOwner(userId, fromStr, toStr);
            case MANAGER -> reportRepository.incomeByManager(userId, fromStr, toStr);
            default      -> throw new BillingAccessDeniedException(
                    "role " + role + " is not permitted to access income reports");
        };

        return rows.stream().map(r -> toIncomeRow(r, from, to)).toList();
    }

    // ── EXPENSES ─────────────────────────────────────────────────────────────

    /**
     * Expense report: total late fees assessed per property.
     *
     * @param from  optional start date (inclusive), null = unbounded
     * @param to    optional end date (inclusive), null = unbounded
     */
    @Transactional(readOnly = true)
    public List<ExpenseReportRow> getExpenseReport(LocalDate from, LocalDate to) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role   = SecurityUtil.getCurrentUserRole();

        String fromStr = from != null ? from.toString() : null;
        String toStr   = to   != null ? to.toString()   : null;

        List<Object[]> rows = switch (role) {
            case OWNER   -> reportRepository.expensesByOwner(userId, fromStr, toStr);
            case MANAGER -> reportRepository.expensesByManager(userId, fromStr, toStr);
            default      -> throw new BillingAccessDeniedException(
                    "role " + role + " is not permitted to access expense reports");
        };

        return rows.stream().map(r -> toExpenseRow(r, from, to)).toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private IncomeReportRow toIncomeRow(Object[] r, LocalDate from, LocalDate to) {
        return new IncomeReportRow(
                ((Number) r[0]).longValue(),   // propertyId
                (String)  r[1],               // propertyName
                (String)  r[2],               // propertyAddress
                from,
                to,
                toBigDecimal(r[3]),            // totalIncome
                ((Number) r[4]).longValue()    // paymentCount
        );
    }

    private ExpenseReportRow toExpenseRow(Object[] r, LocalDate from, LocalDate to) {
        return new ExpenseReportRow(
                ((Number) r[0]).longValue(),   // propertyId
                (String)  r[1],               // propertyName
                (String)  r[2],               // propertyAddress
                from,
                to,
                toBigDecimal(r[3]),            // totalLateFees
                ((Number) r[4]).longValue()    // lateFeeCount
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n)     return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }
}
