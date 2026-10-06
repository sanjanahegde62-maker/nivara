package com.example.rental_management.scheduler.service;

import com.example.rental_management.scheduler.entity.JobExecutionLog;
import com.example.rental_management.scheduler.repository.JobExecutionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.IntSupplier;

/**
 * Idempotency wrapper for all scheduled jobs.
 *
 * Usage:
 * <pre>
 *   scheduledJobService.runIfNotAlreadyRan("RentChargeGenerationJob", () -> {
 *       return myJobLogic.execute();   // returns number of rows affected
 *   });
 * </pre>
 *
 * Guarantees:
 * - A job with the same (jobName, today) pair runs at most once per calendar day.
 * - The guarantee holds even if two scheduler threads fire simultaneously:
 *   the UNIQUE DB constraint on (job_name, run_date) ensures only one wins.
 * - The log entry is written in its own REQUIRES_NEW transaction so it is
 *   committed even if the caller's outer transaction rolls back.
 */
@Service
public class ScheduledJobService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobService.class);

    private final JobExecutionLogRepository logRepository;

    public ScheduledJobService(JobExecutionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Run {@code work} if and only if no SUCCESS log row exists for
     * (jobName, today).  Returns the number of rows affected (0 if skipped).
     *
     * @param jobName unique name identifying the job
     * @param work    supplier that performs the work and returns rows affected
     * @return rows affected, or -1 if the job was skipped
     */
    public int runIfNotAlreadyRan(String jobName, IntSupplier work) {
        LocalDate today = LocalDate.now();

        if (logRepository.existsByJobNameAndRunDate(jobName, today)) {
            log.info("[{}] already ran today ({}), skipping.", jobName, today);
            return -1;
        }

        JobExecutionLog entry = new JobExecutionLog();
        entry.setJobName(jobName);
        entry.setRunDate(today);
        entry.setStartedAt(LocalDateTime.now());
        entry.setRowsAffected(0);
        entry.setStatus("FAILED"); // will be overwritten on success

        try {
            int affected = work.getAsInt();
            entry.setStatus("SUCCESS");
            entry.setRowsAffected(affected);
            log.info("[{}] completed: {} row(s) affected.", jobName, affected);
            return affected;
        } catch (Exception ex) {
            entry.setStatus("FAILED");
            log.error("[{}] failed: {}", jobName, ex.getMessage(), ex);
            throw ex;
        } finally {
            entry.setFinishedAt(LocalDateTime.now());
            try {
                persistLog(entry);
            } catch (DataIntegrityViolationException dupe) {
                // Lost the race to another thread — acceptable, both did the same work
                log.warn("[{}] duplicate log entry race on {}, ignoring.", jobName, today);
            }
        }
    }

    /**
     * Persist the log entry in a REQUIRES_NEW transaction so it is saved
     * independently of the caller's transaction outcome.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistLog(JobExecutionLog entry) {
        logRepository.save(entry);
    }
}
