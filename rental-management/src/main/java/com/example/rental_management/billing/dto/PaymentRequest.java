package com.example.rental_management.billing.dto;

import java.math.BigDecimal;

/**
 * Inbound request body for POST /payments.
 */
public record PaymentRequest(
        Long rentChargeId,
        BigDecimal amount,
        String paymentMethod,
        String reference
) {}
