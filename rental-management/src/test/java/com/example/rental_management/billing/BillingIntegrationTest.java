package com.example.rental_management.billing;

import com.example.rental_management.billing.dto.LeaseChargesSummary;
import com.example.rental_management.billing.dto.PaymentRequest;
import com.example.rental_management.billing.dto.RentChargeResponse;
import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.exception.BillingAccessDeniedException;
import com.example.rental_management.billing.exception.DuplicateChargeException;
import com.example.rental_management.billing.exception.InvalidPaymentException;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.billing.repository.PaymentRepository;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.billing.service.NewBillingService;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.support.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the billing module.
 *
 * Tests run against the real PostgreSQL database (same credentials as the app).
 * Each test is wrapped in a transaction that is rolled back after the test,
 * so no test data persists between tests.
 *
 * All lazy associations are accessible within the test transaction.
 *
 * Scenarios covered:
 *  1.  Tenant can see own charges
 *  2.  Tenant cannot see another tenant's charges
 *  3.  Owner can see charges for owned properties
 *  4.  Manager cannot see unrelated property billing
 *  5.  Maintenance staff cannot access billing
 *  6.  Duplicate charge for same lease/month is rejected
 *  7.  Payment updates charge status to PAID
 *  8.  Payment creates a ledger PAYMENT entry
 *  9.  Outstanding balance is correct after partial payment
 * 10.  Zero payment is rejected
 * 11.  Overpayment is rejected
 * 12.  Paid charge cannot be paid again
 * 13.  Tenant cannot pay another tenant's charge
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillingIntegrationTest {

    @Autowired NewBillingService billingService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired RentChargeRepository rentChargeRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired TestFixtures fixtures;

    // ── Security helpers ──────────────────────────────────────────────────────

    private void authenticate(Long userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ── Data helper ───────────────────────────────────────────────────────────

    /**
     * Persist a PENDING rent charge directly (bypasses authorization).
     * Data is rolled back automatically after each test.
     * Uses distinct past months to avoid unique constraint violations within a test.
     */
    private RentCharge createTestCharge(Lease lease, YearMonth month, BigDecimal amount) {
        RentCharge charge = new RentCharge();
        charge.setLease(lease);
        charge.setBillingPeriod(month.atDay(1));
        charge.setAmount(amount);
        charge.setDueDate(month.atDay(1));
        charge.setStatus(ChargeStatus.PENDING);
        return rentChargeRepository.save(charge);
    }

    // ── Base fixture ──────────────────────────────────────────────────────────

    /**
     * Returns the canonical ACTIVE lease used across all tests.
     * leaseId=1 is pre-existing test data (same convention as ScheduledJobIntegrationTest).
     */
    private Lease activeLease() {
        return fixtures.createActiveLease();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Tenant can see own charges
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void tenant_canSeeOwnCharges() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();

        // Use a billing month unlikely to conflict with today's scheduler tests
        YearMonth month = YearMonth.of(2020, 1);

        // Owner creates the charge
        authenticate(ownerId, "OWNER");
        RentChargeResponse created = billingService.createRentCharge(lease.getId(), month);

        // Tenant can see it
        authenticate(tenantId, "TENANT");
        LeaseChargesSummary summary = billingService.getChargesForLease(lease.getId());

        assertThat(summary.leaseId()).isEqualTo(lease.getId());
        assertThat(summary.charges()).anyMatch(c -> c.id().equals(created.id()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Tenant cannot see another tenant's charges
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void tenant_cannotSeeAnotherTenantsCharges() {
        Lease lease = activeLease();
        Long realTenantId = lease.getTenant().getId();
        Long impostor     = realTenantId + 9999L; // guaranteed different

        authenticate(impostor, "TENANT");
        assertThatThrownBy(() -> billingService.getChargesForLease(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — Owner can see charges for owned properties
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void owner_canSeeChargesForOwnedLease() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 2), lease.getMonthlyRent());

        authenticate(ownerId, "OWNER");
        LeaseChargesSummary summary = billingService.getChargesForLease(lease.getId());

        assertThat(summary.charges()).anyMatch(c -> c.id().equals(charge.getId()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 4 — Manager cannot see unrelated property billing
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void manager_cannotSeeUnrelatedPropertyBilling() {
        Lease lease = activeLease();
        Long unrelatedManagerId = 99999L; // no assignment in test DB

        authenticate(unrelatedManagerId, "MANAGER");
        assertThatThrownBy(() -> billingService.getChargesForLease(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 5 — Maintenance staff cannot access billing
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void maintenanceStaff_cannotAccessBilling() {
        Lease lease = activeLease();

        authenticate(99L, "MAINTENANCE_STAFF");
        assertThatThrownBy(() -> billingService.getChargesForLease(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 6 — Duplicate charge for same lease/month is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void duplicateCharge_isRejected() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();
        YearMonth month = YearMonth.of(2020, 3);

        // Create the first charge directly
        createTestCharge(lease, month, lease.getMonthlyRent());

        // Attempting to create another for the same month via the service must throw
        authenticate(ownerId, "OWNER");
        assertThatThrownBy(() -> billingService.createRentCharge(lease.getId(), month))
                .isInstanceOf(DuplicateChargeException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 7 — Full payment updates charge status to PAID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void payment_updatesChargeStatusToPaid() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 4), rent);

        authenticate(tenantId, "TENANT");
        billingService.recordPayment(new PaymentRequest(charge.getId(), rent, "UPI", "TXN_TEST_7"));

        RentCharge updated = rentChargeRepository.findById(charge.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ChargeStatus.PAID);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 8 — Payment creates a PAYMENT ledger entry
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void payment_createsLedgerEntry() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 5), rent);

        long countBefore = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId())
                .stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.PAYMENT)
                .count();

        authenticate(tenantId, "TENANT");
        billingService.recordPayment(new PaymentRequest(charge.getId(), rent, "BANK_TRANSFER", "TXN_TEST_8"));

        long countAfter = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId())
                .stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.PAYMENT)
                .count();

        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 9 — Outstanding balance is correct after partial payment
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void outstandingBalance_isCorrectAfterPartialPayment() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();
        BigDecimal rent    = lease.getMonthlyRent();
        BigDecimal partial = rent.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 6), rent);

        // Tenant makes a partial payment
        authenticate(tenantId, "TENANT");
        billingService.recordPayment(new PaymentRequest(charge.getId(), partial, "CASH", "TXN_TEST_9"));

        // Owner verifies the balance
        authenticate(ownerId, "OWNER");
        LeaseChargesSummary summary = billingService.getChargesForLease(lease.getId());

        // totalPaid must include at least the partial payment
        assertThat(summary.totalPaid()).isGreaterThanOrEqualTo(partial);
        // outstandingBalance must include at least the unpaid half
        assertThat(summary.outstandingBalance()).isGreaterThanOrEqualTo(rent.subtract(partial));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 10 — Zero payment is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void zeroPayment_isRejected() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 7), lease.getMonthlyRent());

        authenticate(tenantId, "TENANT");
        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(charge.getId(), BigDecimal.ZERO, "UPI", null)))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 11 — Overpayment is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    void overpayment_isRejected() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 8), rent);

        authenticate(tenantId, "TENANT");
        BigDecimal overAmount = rent.add(new BigDecimal("1.00"));
        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(charge.getId(), overAmount, "UPI", null)))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 12 — Paid charge cannot be paid again
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    void paidCharge_cannotBePaidAgain() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 9), rent);

        authenticate(tenantId, "TENANT");
        // Pay in full
        billingService.recordPayment(new PaymentRequest(charge.getId(), rent, "UPI", "TXN_12A"));
        // Second attempt
        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(
                        charge.getId(), new BigDecimal("1.00"), "UPI", "TXN_12B")))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 13 — Tenant cannot pay another tenant's charge
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(13)
    void tenant_cannotPayAnotherTenantsCharge() {
        Lease lease = activeLease();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createTestCharge(lease, YearMonth.of(2020, 10), rent);
        Long impostorTenantId = lease.getTenant().getId() + 9999L;

        authenticate(impostorTenantId, "TENANT");
        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(charge.getId(), rent, "UPI", "TXN_13")))
                .isInstanceOf(BillingAccessDeniedException.class);
    }
}
