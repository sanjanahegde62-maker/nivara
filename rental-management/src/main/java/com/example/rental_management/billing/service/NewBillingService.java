package com.example.rental_management.billing.service;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.billing.dto.*;
import com.example.rental_management.billing.entity.*;
import com.example.rental_management.billing.exception.*;
import com.example.rental_management.billing.repository.*;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.property.entity.PropertyManager;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.user.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Core billing workflow service.
 *
 * All operations that mutate more than one row are annotated @Transactional so
 * they succeed or fail atomically.  Read-only methods use readOnly=true for a
 * small performance benefit and to prevent accidental writes inside a query.
 */
@Service
public class NewBillingService {

    private final LeaseRepository leaseRepository;
    private final RentChargeRepository rentChargeRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public NewBillingService(
            LeaseRepository leaseRepository,
            RentChargeRepository rentChargeRepository,
            PaymentRepository paymentRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PropertyManagerRepository propertyManagerRepository) {
        this.leaseRepository = leaseRepository;
        this.rentChargeRepository = rentChargeRepository;
        this.paymentRepository = paymentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    // ── RENT CHARGES ─────────────────────────────────────────────────────────

    /**
     * Create a rent charge for a lease and billing month.
     * Only OWNER and MANAGER roles may create charges (not TENANT).
     * Rejects duplicate charge for the same lease + billing period at the service level;
     * the database unique constraint is the final safety net.
     */
    @Transactional
    public RentChargeResponse createRentCharge(Long leaseId, YearMonth billingMonth) {
        Lease lease = requireLease(leaseId);

        // Only owners/managers may generate charges
        authorizeChargeCreation(lease);

        if (!"ACTIVE".equals(lease.getStatus())) {
            throw new IllegalStateException(
                    "rent charges can only be created for an active lease");
        }

        LocalDate periodDate = RentCharge.periodFrom(billingMonth);

        if (rentChargeRepository.existsByLeaseIdAndBillingPeriod(leaseId, periodDate)) {
            throw new DuplicateChargeException(
                    "a rent charge already exists for lease " + leaseId
                    + " and billing period " + billingMonth);
        }

        // Due date is the 1st of the billing month (or override as needed)
        LocalDate dueDate = billingMonth.atDay(1);

        RentCharge charge = new RentCharge();
        charge.setLease(lease);
        charge.setBillingPeriod(periodDate);
        charge.setAmount(lease.getMonthlyRent());
        charge.setDueDate(dueDate);
        charge.setStatus(ChargeStatus.PENDING);
        charge = rentChargeRepository.save(charge);

        // Ledger: debit entry for the new charge
        LedgerEntry entry = new LedgerEntry();
        entry.setLease(lease);
        entry.setEntryType(LedgerEntryType.RENT_CHARGE);
        entry.setAmount(charge.getAmount());
        entry.setDescription("Rent charge for " + billingMonth);
        entry.setRentChargeId(charge.getId());
        ledgerEntryRepository.save(entry);

        return RentChargeResponse.from(charge);
    }

    /**
     * Return all charges for a lease with the calculated balance summary.
     * Row-level authorisation is enforced: tenants see only their own lease,
     * owners see only leases for their properties, managers only their assignments.
     */
    @Transactional(readOnly = true)
    public LeaseChargesSummary getChargesForLease(Long leaseId) {
        Lease lease = requireLease(leaseId);
        authorizeLeaseRead(lease);

        List<RentCharge> charges =
                rentChargeRepository.findByLeaseIdOrderByBillingPeriodDesc(leaseId);

        List<RentChargeResponse> responseList = charges.stream()
                .map(RentChargeResponse::from)
                .toList();

        // Calculate balance from charges + payments (no mutable balance field)
        BigDecimal totalCharged = charges.stream()
                .map(RentCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = charges.stream()
                .map(rc -> paymentRepository.sumCompletedByRentChargeId(rc.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = totalCharged.subtract(totalPaid);

        return new LeaseChargesSummary(leaseId, totalCharged, totalPaid, outstanding, responseList);
    }

    // ── PAYMENTS ─────────────────────────────────────────────────────────────

    /**
     * Record a payment against a rent charge.
     *
     * Atomic steps:
     * 1. Load and validate the charge
     * 2. Authorise: only the tenant for this lease may pay
     * 3. Validate the payment amount (positive, not exceeding outstanding)
     * 4. Create the Payment record
     * 5. Update the RentCharge status (PAID when fully settled)
     * 6. Create the ledger credit entry
     */
    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        if (request.rentChargeId() == null) {
            throw new IllegalArgumentException("rentChargeId is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("payment amount must be greater than zero");
        }
        if (request.paymentMethod() == null || request.paymentMethod().isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required");
        }

        RentCharge charge = rentChargeRepository.findById(request.rentChargeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "rent charge not found: " + request.rentChargeId()));

        Lease lease = charge.getLease();

        // Only the tenant who owns this lease may make payments
        authorizeTenantPayment(lease);

        if (charge.getStatus() == ChargeStatus.PAID) {
            throw new InvalidPaymentException(
                    "charge " + charge.getId() + " is already fully paid");
        }

        // How much has already been paid against this charge?
        BigDecimal alreadyPaid = paymentRepository.sumCompletedByRentChargeId(charge.getId());
        BigDecimal outstanding = charge.getAmount().subtract(alreadyPaid);

        if (request.amount().compareTo(outstanding) > 0) {
            throw new InvalidPaymentException(
                    "payment amount " + request.amount()
                    + " exceeds outstanding balance of " + outstanding);
        }

        // 4. Create payment
        Payment payment = new Payment();
        payment.setRentCharge(charge);
        payment.setAmount(request.amount());
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReference(request.reference());
        payment = paymentRepository.save(payment);

        // 5. Update charge status
        BigDecimal newTotalPaid = alreadyPaid.add(request.amount());
        if (newTotalPaid.compareTo(charge.getAmount()) == 0) {
            charge.setStatus(ChargeStatus.PAID);
        }
        // If partial, status stays PENDING (or OVERDUE) — deliberately unchanged
        rentChargeRepository.save(charge);

        // 6. Ledger credit entry
        LedgerEntry entry = new LedgerEntry();
        entry.setLease(lease);
        entry.setEntryType(LedgerEntryType.PAYMENT);
        entry.setAmount(request.amount());
        entry.setDescription("Payment via " + request.paymentMethod()
                + (request.reference() != null ? " ref: " + request.reference() : ""));
        entry.setRentChargeId(charge.getId());
        entry.setPaymentId(payment.getId());
        ledgerEntryRepository.save(entry);

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "payment not found: " + paymentId));

        // Authorise: only roles with access to the lease may read the payment
        authorizeLeaseRead(payment.getRentCharge().getLease());

        return PaymentResponse.from(payment);
    }

    // ── LEDGER ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedger(Long leaseId) {
        Lease lease = requireLease(leaseId);
        authorizeLeaseRead(lease);

        return ledgerEntryRepository.findByLeaseIdOrderByCreatedAtAsc(leaseId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    // ── LATE FEES ────────────────────────────────────────────────────────────

    /**
     * Mark overdue charges and add a late-fee ledger entry.
     * Designed to be called from a scheduler — does NOT require an authenticated user.
     * The fee is 1 % of the outstanding balance per day overdue.
     * The original charge amount is never modified.
     */
    @Transactional
    public void applyLateFees() {
        List<RentCharge> overdue = rentChargeRepository.findChargesToMarkOverdue(LocalDate.now());

        for (RentCharge charge : overdue) {
            charge.setStatus(ChargeStatus.OVERDUE);
            rentChargeRepository.save(charge);

            BigDecimal alreadyPaid = paymentRepository.sumCompletedByRentChargeId(charge.getId());
            BigDecimal outstanding = charge.getAmount().subtract(alreadyPaid);

            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // fully paid edge case
            }

            long overdueDays = java.time.temporal.ChronoUnit.DAYS
                    .between(charge.getDueDate(), LocalDate.now());

            if (overdueDays <= 0) {
                continue; // due date is today — no days elapsed yet
            }

            BigDecimal lateFee = outstanding
                    .multiply(new BigDecimal("0.01"))
                    .multiply(BigDecimal.valueOf(overdueDays));

            LedgerEntry feeEntry = new LedgerEntry();
            feeEntry.setLease(charge.getLease());
            feeEntry.setEntryType(LedgerEntryType.LATE_FEE);
            feeEntry.setAmount(lateFee);
            feeEntry.setDescription("Late fee: " + overdueDays + " day(s) overdue on charge "
                    + charge.getId());
            feeEntry.setRentChargeId(charge.getId());
            ledgerEntryRepository.save(feeEntry);
        }
    }

    // ── AUTHORIZATION ────────────────────────────────────────────────────────

    /**
     * Only OWNER and MANAGER may create rent charges.
     */
    private void authorizeChargeCreation(Lease lease) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();

        switch (role) {
            case OWNER -> {
                if (!userId.equals(lease.getUnit().getProperty().getOwner().getId())) {
                    throw new BillingAccessDeniedException(
                            "owner is not authorised to manage this lease");
                }
            }
            case MANAGER -> {
                Long propertyId = lease.getUnit().getProperty().getId();
                propertyManagerRepository.findByPropertyIdAndManagerId(propertyId, userId)
                        .orElseThrow(() -> new BillingAccessDeniedException(
                                "manager is not assigned to this property"));
            }
            default -> throw new BillingAccessDeniedException(
                    "only OWNER or MANAGER may create rent charges");
        }
    }

    /**
     * Row-level read authorisation.
     * TENANT: may only read their own lease.
     * OWNER: may only read leases for properties they own.
     * MANAGER: may only read leases for properties assigned to them.
     * MAINTENANCE_STAFF: denied.
     */
    private void authorizeLeaseRead(Lease lease) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();

        switch (role) {
            case TENANT -> {
                if (!userId.equals(lease.getTenant().getId())) {
                    throw new BillingAccessDeniedException(
                            "tenant is not authorised to access this lease billing");
                }
            }
            case OWNER -> {
                if (!userId.equals(lease.getUnit().getProperty().getOwner().getId())) {
                    throw new BillingAccessDeniedException(
                            "owner is not authorised to access this lease billing");
                }
            }
            case MANAGER -> {
                Long propertyId = lease.getUnit().getProperty().getId();
                propertyManagerRepository.findByPropertyIdAndManagerId(propertyId, userId)
                        .orElseThrow(() -> new BillingAccessDeniedException(
                                "manager is not assigned to this property"));
            }
            case MAINTENANCE_STAFF -> throw new BillingAccessDeniedException(
                    "maintenance staff do not have access to billing");
        }
    }

    /**
     * A payment may only be submitted by the tenant who owns the lease.
     * Owners and managers may not submit payments on behalf of tenants.
     */
    private void authorizeTenantPayment(Lease lease) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();

        if (role != Role.TENANT) {
            throw new BillingAccessDeniedException(
                    "only the tenant may submit a payment");
        }
        if (!userId.equals(lease.getTenant().getId())) {
            throw new BillingAccessDeniedException(
                    "tenant is not authorised to pay charges for this lease");
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Lease requireLease(Long leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "lease not found: " + leaseId));
    }
}
