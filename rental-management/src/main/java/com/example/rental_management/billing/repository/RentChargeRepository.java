package com.example.rental_management.billing.repository;

import com.example.rental_management.billing.entity.ChargeStatus;
import com.example.rental_management.billing.entity.RentCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentChargeRepository extends JpaRepository<RentCharge, Long> {

    /** All charges for a lease, newest first. */
    List<RentCharge> findByLeaseIdOrderByBillingPeriodDesc(Long leaseId);

    /** Duplicate-guard: does a charge already exist for this lease + month? */
    boolean existsByLeaseIdAndBillingPeriod(Long leaseId, LocalDate billingPeriod);

    /**
     * Charges that have passed their due date and are still PENDING,
     * used by the overdue-marking scheduler.
     */
    @Query("""
        SELECT rc
        FROM RentCharge rc
        WHERE rc.dueDate < :today
          AND rc.status = com.example.rental_management.billing.entity.ChargeStatus.PENDING
    """)
    List<RentCharge> findChargesToMarkOverdue(@Param("today") LocalDate today);

    /**
     * Charges that are currently OVERDUE (for the LateFeeJob).
     * The overdue-detection job runs first and transitions PENDING→OVERDUE,
     * so LateFeeJob only processes already-OVERDUE charges.
     */
    @Query("""
        SELECT rc
        FROM RentCharge rc
        WHERE rc.status = com.example.rental_management.billing.entity.ChargeStatus.OVERDUE
    """)
    List<RentCharge> findAllOverdueCharges();

    /**
     * All charges for leases belonging to properties owned by the given owner.
     * Used for owner-level row authorisation during reads.
     */
    @Query("""
        SELECT rc
        FROM RentCharge rc
        JOIN rc.lease l
        JOIN l.unit u
        JOIN u.property p
        WHERE p.owner.id = :ownerId
          AND l.id = :leaseId
    """)
    List<RentCharge> findByLeaseIdAndOwner(
            @Param("leaseId") Long leaseId,
            @Param("ownerId") Long ownerId);

    /**
     * All charges for leases belonging to properties managed by the given manager.
     */
    @Query("""
        SELECT rc
        FROM RentCharge rc
        JOIN rc.lease l
        JOIN l.unit u
        JOIN u.property p
        JOIN PropertyManager pm ON pm.property = p
        WHERE pm.manager.id = :managerId
          AND l.id = :leaseId
    """)
    List<RentCharge> findByLeaseIdAndManager(
            @Param("leaseId") Long leaseId,
            @Param("managerId") Long managerId);
}
