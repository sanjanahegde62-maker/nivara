package com.example.rental_management.billing.controller;

import com.example.rental_management.billing.dto.*;
import com.example.rental_management.billing.service.NewBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
/**
 * REST API for the billing module.
 *
 * All endpoints are authenticated (JWT required — enforced by SecurityConfig).
 * Row-level authorisation is delegated to NewBillingService.
 *
 * Routes:
 *   POST   /leases/{leaseId}/charges            — create a rent charge
 *   GET    /leases/{leaseId}/charges            — list charges + balance summary
 *   GET    /leases/{leaseId}/ledger             — full ledger for a lease
 *   POST   /payments                            — record a payment
 *   GET    /payments/{paymentId}                — get payment detail
 */
@RestController
public class BillingController {

    private final NewBillingService billingService;

    public BillingController(NewBillingService billingService) {
        this.billingService = billingService;
    }

    // ── Rent charges ─────────────────────────────────────────────────────────

    /**
     * Create a rent charge for a given lease and billing month.
     * billingMonth format: yyyy-MM  (e.g. 2024-06)
     * Only OWNER and MANAGER may call this endpoint.
     */
    @PostMapping("/leases/{leaseId}/charges")
    public ResponseEntity<RentChargeResponse> createCharge(
            @PathVariable Long leaseId,
            @RequestParam String billingMonth) {

        YearMonth period = YearMonth.parse(billingMonth);
        RentChargeResponse response = billingService.createRentCharge(leaseId, period);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all charges for a lease with the calculated balance summary.
     * TENANT sees only their own lease; OWNER/MANAGER see their respective leases.
     */
    @GetMapping("/leases/{leaseId}/charges")
    public ResponseEntity<LeaseChargesSummary> getCharges(@PathVariable Long leaseId) {
        return ResponseEntity.ok(billingService.getChargesForLease(leaseId));
    }

    /**
     * Full audit ledger for a lease (chronological list of all financial movements).
     */
    @GetMapping("/leases/{leaseId}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable Long leaseId) {
        return ResponseEntity.ok(billingService.getLedger(leaseId));
    }

    // ── Payments ─────────────────────────────────────────────────────────────

    /**
     * Record a payment against a rent charge.
     *
     * Request body:
     * {
     *   "rentChargeId": 1,
     *   "amount": 25000,
     *   "paymentMethod": "UPI",
     *   "reference": "TXN123"
     * }
     *
     * Only the tenant who owns the lease may submit a payment.
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = billingService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a single payment by ID.
     * Caller must have read access to the associated lease.
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(billingService.getPayment(paymentId));
    }
}
