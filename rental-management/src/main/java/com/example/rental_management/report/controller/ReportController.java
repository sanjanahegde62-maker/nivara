package com.example.rental_management.report.controller;

import com.example.rental_management.report.dto.ExpenseReportRow;
import com.example.rental_management.report.dto.IncomeReportRow;
import com.example.rental_management.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting endpoints.
 *
 * All endpoints require JWT authentication (enforced by SecurityConfig).
 * Row-level authorization is delegated to ReportService.
 *
 * GET /reports/income
 *   Returns total completed payment income per property.
 *   Optional query params: from=yyyy-MM-dd, to=yyyy-MM-dd
 *
 * GET /reports/expenses
 *   Returns total late-fee expense per property.
 *   Optional query params: from=yyyy-MM-dd, to=yyyy-MM-dd
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Income report — total rent payments received, grouped by property.
     *
     * @param from optional start date (inclusive), ISO format yyyy-MM-dd
     * @param to   optional end date (inclusive), ISO format yyyy-MM-dd
     */
    @GetMapping("/income")
    public ResponseEntity<List<IncomeReportRow>> incomeReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getIncomeReport(from, to));
    }

    /**
     * Expense report — total late fees assessed, grouped by property.
     *
     * @param from optional start date (inclusive), ISO format yyyy-MM-dd
     * @param to   optional end date (inclusive), ISO format yyyy-MM-dd
     */
    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseReportRow>> expenseReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getExpenseReport(from, to));
    }
}
