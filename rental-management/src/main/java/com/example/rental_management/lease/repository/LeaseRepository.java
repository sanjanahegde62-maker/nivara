package com.example.rental_management.lease.repository;

import com.example.rental_management.lease.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    Optional<Lease> findByApplicationId(Long applicationId);

    List<Lease> findByTenantId(Long tenantId);

    @Query("""
        SELECT l
        FROM Lease l
        JOIN l.unit u
        JOIN u.property p
        WHERE p.owner.id = :ownerId
    """)
    List<Lease> findByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
        SELECT l
        FROM Lease l
        JOIN l.unit u
        JOIN u.property p
        JOIN PropertyManager pm ON pm.property = p
        WHERE pm.manager.id = :managerId
    """)
    List<Lease> findByManagerId(@Param("managerId") Long managerId);

    Optional<Lease> findByTenantIdAndUnitIdAndStatus(Long tenanttId, Long unitId, String status);

    /** All active leases — used by the rent-charge generation job. */
    List<Lease> findByStatus(String status);

    /**
     * Active leases whose end date falls between today (inclusive) and
     * the given horizon date (inclusive).  Used by the lease-expiry job
     * to send advance-notice reminders.
     */
    @Query("""
        SELECT l
        FROM Lease l
        WHERE l.status = 'ACTIVE'
          AND l.endDate >= :today
          AND l.endDate <= :horizon
    """)
    List<Lease> findActiveExpiringBetween(
            @Param("today") LocalDate today,
            @Param("horizon") LocalDate horizon);

    /**
     * Active leases whose end date has already passed.
     * Used by the lease-expiry job to transition them to EXPIRED.
     */
    @Query("""
        SELECT l
        FROM Lease l
        WHERE l.status = 'ACTIVE'
          AND l.endDate < :today
    """)
    List<Lease> findActiveExpired(@Param("today") LocalDate today);

    /**
     * Bulk-update expired leases to EXPIRED status.
     * Returns the number of rows updated.
     */
    @Modifying
    @Query("""
        UPDATE Lease l
        SET l.status = 'EXPIRED'
        WHERE l.status = 'ACTIVE'
          AND l.endDate < :today
    """)
    int markExpiredLeases(@Param("today") LocalDate today);
}