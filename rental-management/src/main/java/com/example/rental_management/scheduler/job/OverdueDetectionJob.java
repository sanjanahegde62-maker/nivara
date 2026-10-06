package com.example.rental_management.scheduler.job;

import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.scheduler.service.ScheduledJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Transitions rent charges from PENDING → OVERDUE when their due date has passed.
 *
 * Idempotency:
 *   1. ScheduledJobService skips if today's log row already exists.
 *   2. The query only finds PENDING charges — already-OVERDUE charges are never touched.
 *   3. OVERDUE → OVERDUE is therefore never executed.
 *
 * Runs at 00:05 every day (5 minutes after midnight).
 */
@Component
public class OverdueDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueDetectionJob.class);
    public static final String JOB_NAME = "OverdueDetectionJob";

    private final RentChargeRepository rentChargeRepository;
    private final ScheduledJobService scheduledJobService;

    public OverdueDetectionJob(
            RentChargeRepository rentChargeRepository,
            ScheduledJobService scheduledJobService) {
        this.rentChargeRepository = rentChargeRepository;
        this.scheduledJobService = scheduledJobService;
    }

    @Transactional
    public int execute() {
        return scheduledJobService.runIfNotAlreadyRan(JOB_NAME, this::doDetect);
    }

    int doDetect() {
        LocalDate today = LocalDate.now();
        List<RentCharge> toMark = rentChargeRepository.findChargesToMarkOverdue(today);

        for (RentCharge charge : toMark) {
            charge.setStatus(ChargeStatus.OVERDUE);
            rentChargeRepository.save(charge);
            log.info("[{}] charge {} (lease {}) marked OVERDUE, due {}",
                    JOB_NAME, charge.getId(), charge.getLease().getId(), charge.getDueDate());
        }
        return toMark.size();
    }
}
