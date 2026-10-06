package com.example.rental_management.billing.entity;

import com.example.rental_management.lease.entity.Lease;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable audit record of every financial movement for a lease.
 *
 * Entries are NEVER deleted or updated — corrections are made via new entries.
 * This gives an auditable per-lease sub-ledger without full double-entry accounting.
 *
 * entryType   |  sign  |  example
 * ------------|--------|-----------------------------
 * RENT_CHARGE | debit  | Rent for June 2024
 * PAYMENT     | credit | Payment by UPI TXN123
 * LATE_FEE    | debit  | Late fee — 5 days overdue
 */
@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_ledger_lease_id", columnList = "lease_id"),
        @Index(name = "idx_ledger_rent_charge_id", columnList = "rent_charge_id"),
        @Index(name = "idx_ledger_payment_id", columnList = "payment_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    /**
     * Positive amount representing the size of this movement.
     * Whether it increases or decreases the tenant's balance is determined by entryType.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 300)
    private String description;

    /**
     * FK to the rent charge that triggered this entry (null for non-charge entries).
     * Stored as a plain ID to avoid loading the charge graph on every ledger read.
     */
    @Column(name = "rent_charge_id")
    private Long rentChargeId;

    /**
     * FK to the payment that triggered this entry (null for non-payment entries).
     */
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * The calendar date this entry was assessed.
     * Populated for LATE_FEE and LEASE_EXPIRY_NOTICE entries.
     * Used by the partial unique indexes to enforce one entry per charge/lease per day.
     */
    @Column(name = "fee_date")
    private LocalDate feeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
