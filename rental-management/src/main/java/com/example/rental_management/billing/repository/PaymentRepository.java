package com.example.rental_management.billing.repository;

import com.example.rental_management.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** All payments for a given rent charge, ordered by payment date. */
    List<Payment> findByRentChargeIdOrderByPaymentDateAsc(Long rentChargeId);

    /** Sum of all completed payments for a charge (used for balance check). */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.rentCharge.id = :rentChargeId
          AND p.status = com.example.rental_management.billing.entity.PaymentStatus.COMPLETED
    """)
    java.math.BigDecimal sumCompletedByRentChargeId(
            @org.springframework.data.repository.query.Param("rentChargeId") Long rentChargeId);
}
