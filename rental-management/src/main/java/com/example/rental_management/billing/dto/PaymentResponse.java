package com.example.rental_management.billing.dto;

import com.example.rental_management.billing.entity.Payment;
import com.example.rental_management.billing.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe read-only view of a Payment.
 */
public record PaymentResponse(
        Long id,
        Long rentChargeId,
        Long leaseId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        PaymentStatus status,
        String reference,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getRentCharge().getId(),
                p.getRentCharge().getLease().getId(),
                p.getAmount(),
                p.getPaymentDate(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getReference(),
                p.getCreatedAt()
        );
    }
}
