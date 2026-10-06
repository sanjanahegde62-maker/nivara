package com.example.rental_management.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.rental_management.property.entity.Property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregate queries for the reporting module.
 *
 * Uses native SQL for all aggregate report queries because:
 *   - Report queries span multiple joins across unrelated aggregates
 *     (billing_payments → rent_charges → leases → units → properties)
 *   - Native SQL is cleaner, more expressive, and avoids JPQL JOIN/ON limitations
 *   - All financial aggregation happens at the database level — no Java arithmetic
 *
 * Extends JpaRepository<Property, Long> so Spring registers it as a bean
 * without needing its own @Entity.
 */
@Repository
public interface ReportRepository extends JpaRepository<Property, Long> {

    // ── INCOME ────────────────────────────────────────────────────────────────

    /**
     * Total income (COMPLETED billing_payments) per property for a given owner,
     * within an optional date range on payment_date.
     *
     * Returns Object[] rows: [propertyId (Long), propertyName, propertyAddress,
     *                          totalIncome (BigDecimal), paymentCount (Long)]
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               COALESCE(SUM(bp.amount), 0)  AS total_income,
               COUNT(bp.id)                  AS payment_count
        FROM properties p
        LEFT JOIN units u      ON u.property_id = p.id
        LEFT JOIN leases l     ON l.unit_id     = u.id
        LEFT JOIN rent_charges rc ON rc.lease_id = l.id
        LEFT JOIN billing_payments bp
               ON bp.rent_charge_id = rc.id
              AND bp.status         = 'COMPLETED'
              AND (CAST(:from AS date) IS NULL OR bp.payment_date >= CAST(:from AS date))
              AND (CAST(:to   AS date) IS NULL OR bp.payment_date <= CAST(:to   AS date))
        WHERE p.owner_id = :ownerId
        GROUP BY p.id, p.name, p.address
        ORDER BY p.id
    """, nativeQuery = true)
    List<Object[]> incomeByOwner(
            @Param("ownerId") Long ownerId,
            @Param("from") String from,
            @Param("to") String to);

    /**
     * Total income per property for a given manager (via property_managers assignment),
     * within an optional date range on payment_date.
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               COALESCE(SUM(bp.amount), 0)  AS total_income,
               COUNT(bp.id)                  AS payment_count
        FROM properties p
        JOIN property_managers pm ON pm.property_id = p.id AND pm.manager_id = :managerId
        LEFT JOIN units u      ON u.property_id = p.id
        LEFT JOIN leases l     ON l.unit_id     = u.id
        LEFT JOIN rent_charges rc ON rc.lease_id = l.id
        LEFT JOIN billing_payments bp
               ON bp.rent_charge_id = rc.id
              AND bp.status         = 'COMPLETED'
              AND (CAST(:from AS date) IS NULL OR bp.payment_date >= CAST(:from AS date))
              AND (CAST(:to   AS date) IS NULL OR bp.payment_date <= CAST(:to   AS date))
        GROUP BY p.id, p.name, p.address
        ORDER BY p.id
    """, nativeQuery = true)
    List<Object[]> incomeByManager(
            @Param("managerId") Long managerId,
            @Param("from") String from,
            @Param("to") String to);

    // ── EXPENSES (late fees) ──────────────────────────────────────────────────

    /**
     * Total late fees per property for a given owner,
     * within an optional date range on ledger entry created_at.
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               COALESCE(SUM(le.amount), 0)  AS total_late_fees,
               COUNT(le.id)                  AS late_fee_count
        FROM properties p
        LEFT JOIN units u   ON u.property_id = p.id
        LEFT JOIN leases l  ON l.unit_id     = u.id
        LEFT JOIN ledger_entries le
               ON le.lease_id   = l.id
              AND le.entry_type = 'LATE_FEE'
              AND (CAST(:from AS date) IS NULL OR le.fee_date >= CAST(:from AS date))
              AND (CAST(:to   AS date) IS NULL OR le.fee_date <= CAST(:to   AS date))
        WHERE p.owner_id = :ownerId
        GROUP BY p.id, p.name, p.address
        ORDER BY p.id
    """, nativeQuery = true)
    List<Object[]> expensesByOwner(
            @Param("ownerId") Long ownerId,
            @Param("from") String from,
            @Param("to") String to);

    /**
     * Total late fees per property for a given manager,
     * within an optional date range on fee_date.
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               COALESCE(SUM(le.amount), 0)  AS total_late_fees,
               COUNT(le.id)                  AS late_fee_count
        FROM properties p
        JOIN property_managers pm ON pm.property_id = p.id AND pm.manager_id = :managerId
        LEFT JOIN units u   ON u.property_id = p.id
        LEFT JOIN leases l  ON l.unit_id     = u.id
        LEFT JOIN ledger_entries le
               ON le.lease_id   = l.id
              AND le.entry_type = 'LATE_FEE'
              AND (CAST(:from AS date) IS NULL OR le.fee_date >= CAST(:from AS date))
              AND (CAST(:to   AS date) IS NULL OR le.fee_date <= CAST(:to   AS date))
        GROUP BY p.id, p.name, p.address
        ORDER BY p.id
    """, nativeQuery = true)
    List<Object[]> expensesByManager(
            @Param("managerId") Long managerId,
            @Param("from") String from,
            @Param("to") String to);
}
