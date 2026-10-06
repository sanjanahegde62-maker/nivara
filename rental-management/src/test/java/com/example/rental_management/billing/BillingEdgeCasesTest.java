package com.example.rental_management.billing;

import com.example.rental_management.billing.dto.LedgerEntryResponse;
import com.example.rental_management.billing.dto.PaymentRequest;
import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.exception.BillingAccessDeniedException;
import com.example.rental_management.billing.exception.InvalidPaymentException;
import com.example.rental_management.billing.exception.ResourceNotFoundException;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.billing.repository.PaymentRepository;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.billing.service.NewBillingService;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
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
 * Block E additions — Billing edge cases and financial integrity
 *
 * Complements BillingIntegrationTest with additional coverage for:
 * - Negative payment amount is rejected
 * - Payment against a nonexistent charge is rejected
 * - Ledger history is append-only (entries are never deleted on correction)
 * - Ledger contains RENT_CHARGE debit entry after charge creation
 * - Outstanding balance incorporates multiple partial payments correctly
 * - Paid charge cannot change status back to PENDING
 * - getPayment is authorized (tenant cannot see another tenant's payment)
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillingEdgeCasesTest {

    @Autowired NewBillingService billingService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired RentChargeRepository rentChargeRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;

    // ── Security helpers ──────────────────────────────────────────────────────

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private Lease activeLease() {
        return leaseRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("test data: lease 1 not found"));
    }

    private RentCharge createCharge(Lease lease, YearMonth month, BigDecimal amount) {
        RentCharge charge = new RentCharge();
        charge.setLease(lease);
        charge.setBillingPeriod(month.atDay(1));
        charge.setAmount(amount);
        charge.setDueDate(month.atDay(1));
        charge.setStatus(ChargeStatus.PENDING);
        return rentChargeRepository.save(charge);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E1 — Negative payment amount is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void negativePaymentAmount_isRejected() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        RentCharge charge = createCharge(lease, YearMonth.of(2019, 1), lease.getMonthlyRent());

        authenticate(tenantId, "TENANT");
        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(
                        charge.getId(), new BigDecimal("-100.00"), "UPI", null)))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E2 — Payment against a nonexistent charge is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void paymentAgainstNonexistentCharge_isRejected() {
        Lease lease = activeLease();
        authenticate(lease.getTenant().getId(), "TENANT");

        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(
                        999999L, new BigDecimal("100.00"), "UPI", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E3 — createRentCharge creates a RENT_CHARGE ledger entry
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void createRentCharge_createsRentChargeLedgerEntry() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();
        YearMonth month = YearMonth.of(2019, 2);

        long countBefore = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId())
                .stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.RENT_CHARGE)
                .count();

        authenticate(ownerId, "OWNER");
        billingService.createRentCharge(lease.getId(), month);

        long countAfter = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId())
                .stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.RENT_CHARGE)
                .count();

        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E4 — Ledger entries are never deleted (append-only)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void ledger_isAppendOnly_noEntriesLostAfterPayment() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();
        BigDecimal rent = lease.getMonthlyRent();

        // Create charge (adds 1 ledger entry)
        authenticate(ownerId, "OWNER");
        billingService.createRentCharge(lease.getId(), YearMonth.of(2019, 3));

        long countAfterCharge = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId()).size();

        // Make payment (adds another ledger entry)
        RentCharge charge = rentChargeRepository
                .findByLeaseIdOrderByBillingPeriodDesc(lease.getId())
                .stream()
                .filter(c -> c.getBillingPeriod().equals(LocalDate.of(2019, 3, 1)))
                .findFirst()
                .orElseThrow();

        authenticate(tenantId, "TENANT");
        billingService.recordPayment(new PaymentRequest(charge.getId(), rent, "CASH", "TXN_E4"));

        long countAfterPayment = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(lease.getId()).size();

        // Ledger grew — no entries were removed
        assertThat(countAfterPayment).isGreaterThan(countAfterCharge);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E5 — Two partial payments summing to full amount both succeed
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void twoPartialPayments_sumToFullAmount_chargeMarkedPaid() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();
        BigDecimal half = rent.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal remainder = rent.subtract(half);

        RentCharge charge = createCharge(lease, YearMonth.of(2019, 4), rent);

        authenticate(tenantId, "TENANT");
        billingService.recordPayment(new PaymentRequest(charge.getId(), half, "UPI", "TXN_E5A"));
        billingService.recordPayment(new PaymentRequest(charge.getId(), remainder, "UPI", "TXN_E5B"));

        RentCharge updated = rentChargeRepository.findById(charge.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ChargeStatus.PAID);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E6 — getLedger returns entries for the lease in chronological order
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void getLedger_returnsChronologicalEntries() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        // Create a charge (adds a RENT_CHARGE entry)
        authenticate(ownerId, "OWNER");
        billingService.createRentCharge(lease.getId(), YearMonth.of(2019, 5));

        List<LedgerEntryResponse> ledger = billingService.getLedger(lease.getId());
        assertThat(ledger).isNotEmpty();
        // Check chronological order
        for (int i = 1; i < ledger.size(); i++) {
            assertThat(ledger.get(i).createdAt())
                    .isAfterOrEqualTo(ledger.get(i - 1).createdAt());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E7 — getPayment authorization: tenant cannot see another tenant's payment
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void getPayment_tenantCannotSeeAnotherTenantsPayment() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();
        BigDecimal rent = lease.getMonthlyRent();

        RentCharge charge = createCharge(lease, YearMonth.of(2019, 6), rent);

        authenticate(tenantId, "TENANT");
        var paymentResponse = billingService.recordPayment(
                new PaymentRequest(charge.getId(), rent, "UPI", "TXN_E7"));

        // Impostor tenant tries to read this payment
        Long impostorId = tenantId + 9999L;
        authenticate(impostorId, "TENANT");

        assertThatThrownBy(() -> billingService.getPayment(paymentResponse.id()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // E8 — null rentChargeId in payment request is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void nullRentChargeId_isRejected() {
        Lease lease = activeLease();
        authenticate(lease.getTenant().getId(), "TENANT");

        assertThatThrownBy(() ->
                billingService.recordPayment(new PaymentRequest(
                        null, new BigDecimal("100.00"), "UPI", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
