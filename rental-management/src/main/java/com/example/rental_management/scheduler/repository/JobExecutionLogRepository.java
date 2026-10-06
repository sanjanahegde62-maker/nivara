package com.example.rental_management.scheduler.repository;

import com.example.rental_management.scheduler.entity.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {

    boolean existsByJobNameAndRunDate(String jobName, LocalDate runDate);

    Optional<JobExecutionLog> findByJobNameAndRunDate(String jobName, LocalDate runDate);
}
