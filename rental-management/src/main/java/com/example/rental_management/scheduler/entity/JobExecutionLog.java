package com.example.rental_management.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Audit record of every scheduled-job run.
 *
 * The UNIQUE constraint on (job_name, run_date) is the primary idempotency
 * guard: attempting to insert a duplicate row raises a constraint violation,
 * which the ScheduledJobService catches and converts into a clean skip.
 */
@Entity
@Table(
    name = "job_execution_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_job_run_date",
        columnNames = {"job_name", "run_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 80)
    private String jobName;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "rows_affected", nullable = false)
    private int rowsAffected;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
