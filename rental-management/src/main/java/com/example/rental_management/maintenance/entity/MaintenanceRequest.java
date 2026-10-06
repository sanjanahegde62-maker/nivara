package com.example.rental_management.maintenance.entity;

import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A maintenance request raised by a tenant for a specific unit.
 *
 * Status lifecycle (enforced by service + DB CHECK in V15):
 *   OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
 *
 * SLA columns added by V15:
 *   due_at       — SLA deadline (set on create based on priority)
 *   sla_breached — set to true by SlaBreachJob when due_at passes without RESOLVED
 *   resolved_at  — timestamp when status moved to RESOLVED
 *   closed_at    — timestamp when status moved to CLOSED
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    /** SLA deadline — derived from priority on create. */
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    /**
     * Set to true by the SLA breach job when due_at has passed and the
     * request has not yet reached RESOLVED.
     */
    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached = false;

    /** Populated when status transitions to RESOLVED. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Populated when status transitions to CLOSED. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
