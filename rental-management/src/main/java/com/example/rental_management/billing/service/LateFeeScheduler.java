package com.example.rental_management.billing.service;

import com.example.rental_management.scheduler.job.LateFeeJob;
import com.example.rental_management.scheduler.job.LeaseExpiryJob;
import com.example.rental_management.scheduler.job.OverdueDetectionJob;
import com.example.rental_management.scheduler.job.RentChargeGenerationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron triggers for all four scheduled billing/lease jobs.
 *
 * Execution order each day:
 *   00:05  OverdueDetectionJob   — PENDING → OVERDUE
 *   00:10  LateFeeJob            — add late-fee ledger entry for OVERDUE charges
 *   00:15  LeaseExpiryJob        — expire leases + write reminder notices
 *   06:00  RentChargeGenerationJob — generate charges on the 1st of each month
 *
 * Each job is individually idempotent via ScheduledJobService + DB constraints,
 * so running them twice in a day is safe.
 */
@Component
public class LateFeeScheduler {

    private static final Logger log = LoggerFactory.getLogger(LateFeeScheduler.class);

    private final OverdueDetectionJob overdueDetectionJob;
    private final LateFeeJob lateFeeJob;
    private final LeaseExpiryJob leaseExpiryJob;
    private final RentChargeGenerationJob rentChargeGenerationJob;

    public LateFeeScheduler(
            OverdueDetectionJob overdueDetectionJob,
            LateFeeJob lateFeeJob,
            LeaseExpiryJob leaseExpiryJob,
            RentChargeGenerationJob rentChargeGenerationJob) {
        this.overdueDetectionJob = overdueDetectionJob;
        this.lateFeeJob = lateFeeJob;
        this.leaseExpiryJob = leaseExpiryJob;
        this.rentChargeGenerationJob = rentChargeGenerationJob;
    }

    /** Nightly 00:05 — mark past-due PENDING charges as OVERDUE. */
    @Scheduled(cron = "0 5 0 * * *")
    public void runOverdueDetection() {
        log.info("Scheduled trigger: OverdueDetectionJob");
        overdueDetectionJob.execute();
    }

    /** Nightly 00:10 — apply late fees to OVERDUE charges. */
    @Scheduled(cron = "0 10 0 * * *")
    public void runLateFees() {
        log.info("Scheduled trigger: LateFeeJob");
        lateFeeJob.execute();
    }

    /** Nightly 00:15 — expire leases and write expiry notices. */
    @Scheduled(cron = "0 15 0 * * *")
    public void runLeaseExpiry() {
        log.info("Scheduled trigger: LeaseExpiryJob");
        leaseExpiryJob.execute();
    }

    /** 06:00 on the 1st of each month — generate rent charges for active leases. */
    @Scheduled(cron = "0 0 6 1 * *")
    public void runRentChargeGeneration() {
        log.info("Scheduled trigger: RentChargeGenerationJob");
        rentChargeGenerationJob.execute();
    }
}
