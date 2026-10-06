package com.example.rental_management.scheduler.job;

import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.billing.repository.PaymentRepository;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.scheduler.service.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adds a LATE_FEE ledger entry for every OVERDUE charge.
 *
 * Fee formula: 1 % of outstanding balance × overdue days (assessed per day).
 *
 * Idempotency — two layers:
 *   1. ScheduledJobService: one run per (JOB_NAME, today) in job_execution_log.
 *   2. ledgerEntryRepository.existsByRentChargeIdAndEntryTypeAndFeeDate() check
 *      plus the partial DB unique index on (rent_charge_id, fee_date) for LATE_FEE.
 *      This means even if the job_execution_log check is bypassed, at most one
 *      LATE_FEE per charge per calendar day can ever be inserted.
 *
 * Runs at 00:10 every day (after OverdueDetectionJob at 00:05).
 */
@Component
public class LateFeeJob {

    private static final Logger log = LoggerFactory.getLogger(LateFeeJob.class);
    public static final String JOB_NAME = "LateFeeJob";

    /** Daily late-fee rate: 1% of outstanding per day overdue. */
    private static final BigDecimal DAILY_RATE = new BigDecimal("0.01");

    private final RentChargeRepository rentChargeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentRepository paymentRepository;
    private final ScheduledJobService scheduledJobService;

    public LateFeeJob(
            RentChargeRepository rentChargeRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PaymentRepository paymentRepository,
            ScheduledJobService scheduledJobService) {
        this.rentChargeRepository = rentChargeRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.paymentRepository = paymentRepository;
        this.scheduledJobService = scheduledJobService;
    }

    @Transactional
    public int execute() {
        return scheduledJobService.runIfNotAlreadyRan(JOB_NAME, this::doApply);
    }

    int doApply() {
        LocalDate today = LocalDate.now();
        List<RentCharge> overdue = rentChargeRepository.findAllOverdueCharges();
        AtomicInteger applied = new AtomicInteger(0);

        for (RentCharge charge : overdue) {
            // Layer-2 idempotency guard: already applied today for this charge?
            if (ledgerEntryRepository.existsByRentChargeIdAndEntryTypeAndFeeDate(
                    charge.getId(), LedgerEntryType.LATE_FEE, today)) {
                log.debug("[{}] late fee already recorded for charge {} on {}, skipping.",
                        JOB_NAME, charge.getId(), today);
                continue;
            }

            long overdueDays = ChronoUnit.DAYS.between(charge.getDueDate(), today);
            if (overdueDays <= 0) {
                continue; // due date is today — no days elapsed yet
            }

            BigDecimal alreadyPaid = paymentRepository.sumCompletedByRentChargeId(charge.getId());
            BigDecimal outstanding = charge.getAmount().subtract(alreadyPaid);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // fully paid but status not yet updated — skip
            }

            BigDecimal lateFee = outstanding
                    .multiply(DAILY_RATE)
                    .multiply(BigDecimal.valueOf(overdueDays));

            LedgerEntry feeEntry = new LedgerEntry();
            feeEntry.setLease(charge.getLease());
            feeEntry.setEntryType(LedgerEntryType.LATE_FEE);
            feeEntry.setAmount(lateFee);
            feeEntry.setFeeDate(today);
            feeEntry.setDescription(String.format(
                    "Late fee: %d day(s) overdue on charge %d", overdueDays, charge.getId()));
            feeEntry.setRentChargeId(charge.getId());

            try {
                ledgerEntryRepository.save(feeEntry);
                applied.incrementAndGet();
                log.info("[{}] late fee {} applied for charge {} (lease {}), {} day(s) overdue.",
                        JOB_NAME, lateFee, charge.getId(),
                        charge.getLease().getId(), overdueDays);
            } catch (DataIntegrityViolationException ex) {
                // Race condition hit the partial unique index — safe to ignore
                log.warn("[{}] duplicate late-fee race for charge {} on {}, skipping.",
                        JOB_NAME, charge.getId(), today);
            }
        }
        return applied.get();
    }
}
