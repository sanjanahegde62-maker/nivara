package com.example.rental_management.payment.repository;

import com.example.rental_management.payment.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentPaymentRepository extends JpaRepository<RentPayment,Long> {
    List<RentPayment> findByLeaseId(Long leaseId);
    List<RentPayment> findByLeaseTenantId(Long tenantId);
    boolean existsByLeaseIdAndDueDate(Long leaseId, LocalDate dueDate);

    List<RentPayment> findByStatusAndDueDateBefore(String status,LocalDate date);
    @Query("""
    SELECT p
    FROM RentPayment p
    WHERE p.dueDate < :date
    AND p.status IN ('DUE', 'PARTIALLY_PAID')
""")
    List<RentPayment> findPaymentsToMarkOverdue(
            @Param("date") LocalDate date
    );
}
