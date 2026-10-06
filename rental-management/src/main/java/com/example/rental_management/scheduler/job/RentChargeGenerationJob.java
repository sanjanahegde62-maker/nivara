package com.example.rental_management.scheduler.job;

import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.scheduler.service.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a RentCharge for every ACTIVE lease for the current billing month.
 *
 * Idempotency:
 *   1. ScheduledJobService checks the job_execution_log — if today's row exists, skip entirely.
 *   2. rentChargeRepository.existsByLeaseIdAndBillingPeriod() skips individual charges that
 *      already exist (e.g. manually created by an owner).
 *   3. The DB UNIQUE constraint on (lease_id, billing_period) is the final safety net.
 *
 * Runs at 06:00 on the 1st day of each month.
 */
@Component
public class RentChargeGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(RentChargeGenerationJob.class);
    public static final String JOB_NAME = "RentChargeGenerationJob";

    private final LeaseRepository leaseRepository;
    private final RentChargeRepository rentChargeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ScheduledJobService scheduledJobService;

    public RentChargeGenerationJob(
            LeaseRepository leaseRepository,
            RentChargeRepository rentChargeRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ScheduledJobService scheduledJobService) {
        this.leaseRepository = leaseRepository;
        this.rentChargeRepository = rentChargeRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.scheduledJobService = scheduledJobService;
    }

    @Transactional
    public int execute() {
        return scheduledJobService.runIfNotAlreadyRan(JOB_NAME, this::doGenerate);
    }

    int doGenerate() {
        YearMonth billingMonth = YearMonth.now();
        LocalDate periodDate = billingMonth.atDay(1);
        LocalDate dueDate = billingMonth.atDay(1);

        List<Lease> activeLeases = leaseRepository.findByStatus("ACTIVE");
        AtomicInteger created = new AtomicInteger(0);

        for (Lease lease : activeLeases) {
            if (rentChargeRepository.existsByLeaseIdAndBillingPeriod(lease.getId(), periodDate)) {
                log.debug("[{}] charge already exists for lease {} period {}, skipping.",
                        JOB_NAME, lease.getId(), billingMonth);
                continue;
            }
            try {
                RentCharge charge = new RentCharge();
                charge.setLease(lease);
                charge.setBillingPeriod(periodDate);
                charge.setAmount(lease.getMonthlyRent());
                charge.setDueDate(dueDate);
                charge.setStatus(ChargeStatus.PENDING);
                charge = rentChargeRepository.save(charge);

                LedgerEntry entry = new LedgerEntry();
                entry.setLease(lease);
                entry.setEntryType(LedgerEntryType.RENT_CHARGE);
                entry.setAmount(charge.getAmount());
                entry.setDescription("Rent charge for " + billingMonth + " (auto-generated)");
                entry.setRentChargeId(charge.getId());
                ledgerEntryRepository.save(entry);

                created.incrementAndGet();
                log.info("[{}] created charge for lease {} amount {}",
                        JOB_NAME, lease.getId(), charge.getAmount());

            } catch (DataIntegrityViolationException ex) {
                // Race condition: another thread/process created the same charge
                log.warn("[{}] duplicate race for lease {} period {}, skipping.",
                        JOB_NAME, lease.getId(), billingMonth);
            }
        }
        return created.get();
    }
}
