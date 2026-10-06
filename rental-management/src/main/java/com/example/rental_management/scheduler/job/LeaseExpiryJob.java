package com.example.rental_management.scheduler.job;

import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.scheduler.service.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles two lease-expiry scenarios:
 *
 *   A. Leases that ended yesterday or earlier → status ACTIVE → EXPIRED.
 *      A LEASE_EXPIRY_NOTICE ledger entry is written for audit.
 *
 *   B. Leases expiring within the next 30 days (still ACTIVE) →
 *      A LEASE_EXPIRY_NOTICE reminder is written to the ledger.
 *
 * Idempotency:
 *   1. ScheduledJobService: one run per (JOB_NAME, today) in job_execution_log.
 *   2. ledgerEntryRepository.existsByLeaseIdAndEntryTypeAndFeeDate() prevents
 *      duplicate notice entries; the partial DB unique index is the final guard.
 *   3. markExpiredLeases() is a conditional UPDATE — updating EXPIRED→EXPIRED is
 *      a no-op because the WHERE clause filters on status = 'ACTIVE'.
 *
 * Runs at 00:15 every day.
 */
@Component
public class LeaseExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(LeaseExpiryJob.class);
    public static final String JOB_NAME = "LeaseExpiryJob";

    /** Number of days in advance to write an expiry-reminder notice. */
    private static final int REMINDER_DAYS_AHEAD = 30;

    private final LeaseRepository leaseRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ScheduledJobService scheduledJobService;

    public LeaseExpiryJob(
            LeaseRepository leaseRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ScheduledJobService scheduledJobService) {
        this.leaseRepository = leaseRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.scheduledJobService = scheduledJobService;
    }

    @Transactional
    public int execute() {
        return scheduledJobService.runIfNotAlreadyRan(JOB_NAME, this::doProcess);
    }

    int doProcess() {
        LocalDate today = LocalDate.now();
        AtomicInteger processed = new AtomicInteger(0);

        // ── A. Expire overdue leases ─────────────────────────────────────────
        List<Lease> expired = leaseRepository.findActiveExpired(today);
        for (Lease lease : expired) {
            writeNotice(lease, today,
                    "Lease expired on " + lease.getEndDate() + " — status set to EXPIRED");
            processed.incrementAndGet();
        }
        int expiredCount = leaseRepository.markExpiredLeases(today);
        log.info("[{}] {} lease(s) transitioned to EXPIRED.", JOB_NAME, expiredCount);

        // ── B. Reminder notices for leases expiring soon ──────────────────────
        LocalDate horizon = today.plusDays(REMINDER_DAYS_AHEAD);
        List<Lease> expiringSoon = leaseRepository.findActiveExpiringBetween(today, horizon);

        for (Lease lease : expiringSoon) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS
                    .between(today, lease.getEndDate());
            writeNotice(lease, today,
                    "Lease expiry reminder: " + daysLeft + " day(s) remaining (ends "
                    + lease.getEndDate() + ")");
            processed.incrementAndGet();
        }
        return processed.get();
    }

    /** Write a LEASE_EXPIRY_NOTICE ledger entry unless one already exists today. */
    private void writeNotice(Lease lease, LocalDate today, String description) {
        if (ledgerEntryRepository.existsByLeaseIdAndEntryTypeAndFeeDate(
                lease.getId(), LedgerEntryType.LEASE_EXPIRY_NOTICE, today)) {
            log.debug("[{}] notice already written for lease {} on {}, skipping.",
                    JOB_NAME, lease.getId(), today);
            return;
        }
        LedgerEntry notice = new LedgerEntry();
        notice.setLease(lease);
        notice.setEntryType(LedgerEntryType.LEASE_EXPIRY_NOTICE);
        // Amount must be > 0 per DB constraint — use 1.00 as a nominal placeholder
        notice.setAmount(BigDecimal.ONE);
        notice.setDescription(description);
        notice.setFeeDate(today);
        try {
            ledgerEntryRepository.save(notice);
            log.info("[{}] notice written for lease {}: {}", JOB_NAME, lease.getId(), description);
        } catch (DataIntegrityViolationException ex) {
            // Partial unique index guard: another thread wrote the same entry
            log.warn("[{}] duplicate notice race for lease {} on {}, skipping.",
                    JOB_NAME, lease.getId(), today);
        }
    }
}
