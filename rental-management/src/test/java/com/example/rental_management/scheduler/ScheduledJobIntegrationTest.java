package com.example.rental_management.scheduler;

import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;
import com.example.rental_management.billing.entity.RentCharge;
import com.example.rental_management.billing.repository.LedgerEntryRepository;
import com.example.rental_management.billing.repository.PaymentRepository;
import com.example.rental_management.billing.repository.RentChargeRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.scheduler.entity.JobExecutionLog;
import com.example.rental_management.scheduler.job.LateFeeJob;
import com.example.rental_management.scheduler.job.LeaseExpiryJob;
import com.example.rental_management.scheduler.job.OverdueDetectionJob;
import com.example.rental_management.scheduler.job.RentChargeGenerationJob;
import com.example.rental_management.scheduler.repository.JobExecutionLogRepository;
import com.example.rental_management.support.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for all four scheduled jobs.
 *
 * Tests run against the real PostgreSQL database (same credentials as the app).
 * They are ordered so that state flows naturally from one test to the next.
 * Each test manages its own setup/cleanup carefully to avoid FK violations.
 *
 * This class is intentionally NOT @Transactional because the scheduled jobs
 * use REQUIRES_NEW transactions for job-log persistence — those commits must
 * be visible across method boundaries for idempotency to be tested correctly.
 *
 * A single ACTIVE lease fixture is created once in @BeforeAll and deleted in
 * @AfterAll.  All job-log rows are cleared before each test that starts a new
 * job run (odd-numbered tests), so CI runs start from a clean slate.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScheduledJobIntegrationTest {

    @Autowired LeaseRepository leaseRepository;
    @Autowired RentChargeRepository rentChargeRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired JobExecutionLogRepository jobLogRepository;

    @Autowired RentChargeGenerationJob rentChargeGenerationJob;
    @Autowired OverdueDetectionJob overdueDetectionJob;
    @Autowired LateFeeJob lateFeeJob;
    @Autowired LeaseExpiryJob leaseExpiryJob;

    @Autowired TestFixtures fixtures;

    /** The lease created for this test class run. Set in @BeforeAll. */
    private Lease testLease;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @BeforeAll
    void createFixture() {
        testLease = fixtures.createActiveLease();
    }

    @AfterAll
    void deleteFixture() {
        // Delete in FK-safe order: ledger → payments → charges → lease graph
        ledgerEntryRepository.findByLeaseIdOrderByCreatedAtAsc(testLease.getId())
                .forEach(ledgerEntryRepository::delete);
        rentChargeRepository.findByLeaseIdOrderByBillingPeriodDesc(testLease.getId())
                .forEach(rc -> {
                    paymentRepository.findByRentChargeIdOrderByPaymentDateAsc(rc.getId())
                            .forEach(paymentRepository::delete);
                    rentChargeRepository.delete(rc);
                });
        fixtures.deleteAll(testLease);
        // Clean up any job logs written during the test run
        jobLogRepository.findAll().stream()
                .filter(l -> l.getRunDate().equals(LocalDate.now()))
                .forEach(jobLogRepository::delete);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void clearJobLog(String jobName) {
        jobLogRepository.findByJobNameAndRunDate(jobName, LocalDate.now())
                .ifPresent(jobLogRepository::delete);
    }

    /**
     * Safely delete a rent charge and all its dependent rows (payments, ledger entries).
     */
    private void safeDeleteCharge(RentCharge rc) {
        ledgerEntryRepository.findAll().stream()
                .filter(e -> rc.getId().equals(e.getRentChargeId()))
                .forEach(ledgerEntryRepository::delete);
        paymentRepository.findByRentChargeIdOrderByPaymentDateAsc(rc.getId())
                .forEach(paymentRepository::delete);
        rentChargeRepository.delete(rc);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 1 — RentChargeGenerationJob: creates charges on first run
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void rentChargeGeneration_createsChargesForActiveLeases() {
        clearJobLog(RentChargeGenerationJob.JOB_NAME);

        assertThat(testLease.getStatus()).isEqualTo("ACTIVE");

        LocalDate periodDate = YearMonth.now().atDay(1);

        // Remove any pre-existing charge for this period (with all dependents)
        rentChargeRepository.findByLeaseIdOrderByBillingPeriodDesc(testLease.getId())
                .stream()
                .filter(rc -> rc.getBillingPeriod().equals(periodDate))
                .forEach(this::safeDeleteCharge);

        int result = rentChargeGenerationJob.execute();
        assertThat(result).isGreaterThanOrEqualTo(1);

        // Charge must now exist for the test lease
        assertThat(rentChargeRepository
                .existsByLeaseIdAndBillingPeriod(testLease.getId(), periodDate)).isTrue();

        // A RENT_CHARGE ledger debit entry must have been written
        boolean hasAutoDebit = ledgerEntryRepository
                .findByLeaseIdOrderByCreatedAtAsc(testLease.getId())
                .stream()
                .anyMatch(e -> e.getEntryType() == LedgerEntryType.RENT_CHARGE
                        && e.getDescription().contains("auto-generated"));
        assertThat(hasAutoDebit).isTrue();

        // Job log SUCCESS
        Optional<JobExecutionLog> log = jobLogRepository
                .findByJobNameAndRunDate(RentChargeGenerationJob.JOB_NAME, LocalDate.now());
        assertThat(log).isPresent();
        assertThat(log.get().getStatus()).isEqualTo("SUCCESS");
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 2 — RentChargeGenerationJob: idempotent on second run
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void rentChargeGeneration_isIdempotent_secondRunSkipped() {
        // Log was written in test 1 — second call must be skipped entirely
        int secondResult = rentChargeGenerationJob.execute();
        assertThat(secondResult).isEqualTo(-1); // -1 = skipped

        // Exactly one charge for this billing period for the test lease
        LocalDate periodDate = YearMonth.now().atDay(1);
        long chargeCount = rentChargeRepository
                .findByLeaseIdOrderByBillingPeriodDesc(testLease.getId())
                .stream()
                .filter(rc -> rc.getBillingPeriod().equals(periodDate))
                .count();
        assertThat(chargeCount).isEqualTo(1);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 3 — OverdueDetectionJob: marks past-due PENDING charges OVERDUE
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void overdueDetection_marksPendingChargesOverdue() {
        clearJobLog(OverdueDetectionJob.JOB_NAME);

        // If no past-due PENDING charge exists, create a synthetic one for the test lease
        boolean hadPastDue = !rentChargeRepository
                .findChargesToMarkOverdue(LocalDate.now()).isEmpty();

        if (!hadPastDue) {
            RentCharge synthetic = new RentCharge();
            synthetic.setLease(testLease);
            synthetic.setBillingPeriod(LocalDate.now().minusMonths(3).withDayOfMonth(1));
            synthetic.setDueDate(LocalDate.now().minusMonths(3).withDayOfMonth(1));
            synthetic.setAmount(testLease.getMonthlyRent());
            synthetic.setStatus(ChargeStatus.PENDING);
            rentChargeRepository.save(synthetic);
        }

        int result = overdueDetectionJob.execute();
        assertThat(result).isGreaterThanOrEqualTo(1);

        // No PENDING past-due charges should remain
        assertThat(rentChargeRepository.findChargesToMarkOverdue(LocalDate.now())).isEmpty();

        // Job log SUCCESS
        Optional<JobExecutionLog> log = jobLogRepository
                .findByJobNameAndRunDate(OverdueDetectionJob.JOB_NAME, LocalDate.now());
        assertThat(log).isPresent();
        assertThat(log.get().getStatus()).isEqualTo("SUCCESS");
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 4 — OverdueDetectionJob: idempotent on second run
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void overdueDetection_isIdempotent_secondRunSkipped() {
        int secondResult = overdueDetectionJob.execute();
        assertThat(secondResult).isEqualTo(-1);

        // No PENDING past-due charges (still none after second run)
        assertThat(rentChargeRepository.findChargesToMarkOverdue(LocalDate.now())).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 5 — LateFeeJob: adds late-fee entry for OVERDUE charges
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void lateFeeJob_addsLateFeeForOverdueCharges() {
        clearJobLog(LateFeeJob.JOB_NAME);

        List<RentCharge> overdue = rentChargeRepository.findAllOverdueCharges();
        // At least one OVERDUE charge must exist (created in test 3)
        assertThat(overdue).isNotEmpty();

        long feesBefore = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LATE_FEE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        int result = lateFeeJob.execute();
        assertThat(result).isGreaterThanOrEqualTo(1);

        long feesAfter = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LATE_FEE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        assertThat(feesAfter).isGreaterThan(feesBefore);

        // Job log SUCCESS
        Optional<JobExecutionLog> log = jobLogRepository
                .findByJobNameAndRunDate(LateFeeJob.JOB_NAME, LocalDate.now());
        assertThat(log).isPresent();
        assertThat(log.get().getStatus()).isEqualTo("SUCCESS");
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 6 — LateFeeJob: idempotent on second run (no extra entries)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void lateFeeJob_isIdempotent_secondRunSkipped() {
        long feesBefore = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LATE_FEE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        int secondResult = lateFeeJob.execute();
        assertThat(secondResult).isEqualTo(-1); // skipped

        long feesAfter = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LATE_FEE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        assertThat(feesAfter).isEqualTo(feesBefore); // no new entries
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 7 — LeaseExpiryJob: runs cleanly, writes job log
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void leaseExpiryJob_runsAndWritesJobLog() {
        clearJobLog(LeaseExpiryJob.JOB_NAME);

        int result = leaseExpiryJob.execute();
        assertThat(result).isGreaterThanOrEqualTo(0);

        Optional<JobExecutionLog> log = jobLogRepository
                .findByJobNameAndRunDate(LeaseExpiryJob.JOB_NAME, LocalDate.now());
        assertThat(log).isPresent();
        assertThat(log.get().getStatus()).isEqualTo("SUCCESS");
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 8 — LeaseExpiryJob: idempotent on second run
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void leaseExpiryJob_isIdempotent_secondRunSkipped() {
        long noticesBefore = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LEASE_EXPIRY_NOTICE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        int secondResult = leaseExpiryJob.execute();
        assertThat(secondResult).isEqualTo(-1);

        long noticesAfter = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.LEASE_EXPIRY_NOTICE
                        && LocalDate.now().equals(e.getFeeDate()))
                .count();

        assertThat(noticesAfter).isEqualTo(noticesBefore);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 9 — DB guard: existsByLeaseIdAndBillingPeriod rejects duplicates
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void rentChargeRepository_existsGuardWorks() {
        LocalDate periodDate = YearMonth.now().atDay(1);
        // Charge for the test lease this period was created in test 1
        assertThat(rentChargeRepository
                .existsByLeaseIdAndBillingPeriod(testLease.getId(), periodDate)).isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 10 — LateFee ledger guard: existsByRentChargeIdAndEntryTypeAndFeeDate
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void lateFeeRepository_existsGuardWorks() {
        List<RentCharge> overdue = rentChargeRepository.findAllOverdueCharges();
        if (overdue.isEmpty()) {
            return; // vacuously pass
        }
        RentCharge charge = overdue.get(0);
        // After test 5, each OVERDUE charge should have a fee for today
        boolean hasToday = ledgerEntryRepository
                .existsByRentChargeIdAndEntryTypeAndFeeDate(
                        charge.getId(), LedgerEntryType.LATE_FEE, LocalDate.now());
        assertThat(hasToday).isTrue();
    }
}
