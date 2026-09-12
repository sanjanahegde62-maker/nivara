package com.example.rental_management.payment.entity;

import com.example.rental_management.lease.entity.Lease;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rent_payments")
@Getter
@Setter
@NoArgsConstructor
public class RentPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "lease_id",nullable = false)
    private Lease lease;
    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;
    @Column(nullable = false)
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    @Column(nullable = false)
    private BigDecimal lateFee=BigDecimal.ZERO;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
        updatedAt=LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdated(){
        updatedAt=LocalDateTime.now();
    }


}
