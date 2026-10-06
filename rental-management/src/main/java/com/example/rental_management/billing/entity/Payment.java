package com.example.rental_management.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records a single payment transaction made against a RentCharge.
 * A charge may receive one or more payments (partial payments), but the
 * total payments must not exceed the charge amount.
 */
@Entity
@Table(name = "billing_payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rent_charge_id", nullable = false)
    private RentCharge rentCharge;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * External transaction reference (e.g. UPI transaction ID, bank reference).
     * Optional — not every payment method generates an external reference.
     */
    @Column(name = "reference", length = 120)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
