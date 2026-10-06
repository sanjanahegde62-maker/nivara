package com.example.rental_management.billing.entity;

import com.example.rental_management.lease.entity.Lease;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Represents a rent amount owed by a tenant for a specific lease and billing period.
 * One record per lease per billing month — enforced by unique constraint.
 */
@Entity
@Table(
    name = "rent_charges",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rent_charge_lease_period",
        columnNames = {"lease_id", "billing_period"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class RentCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    /**
     * The billing month this charge covers, stored as the first day of that month
     * (e.g. 2024-06-01 represents June 2024). Enforces uniqueness with lease_id.
     */
    @Column(name = "billing_period", nullable = false)
    private LocalDate billingPeriod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── convenience factory ──────────────────────────────────────────────────

    /**
     * Derive the billing_period column value from a YearMonth by using the
     * first-of-month date. This keeps the column a plain DATE in SQL while
     * giving the service a type-safe API.
     */
    public static LocalDate periodFrom(YearMonth ym) {
        return ym.atDay(1);
    }

    public YearMonth getBillingYearMonth() {
        return YearMonth.from(billingPeriod);
    }
}
