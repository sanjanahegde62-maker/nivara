package com.example.rental_management.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentPaymentResponse(
        Long id,
        Long leaseId,
        LocalDate dueDate,
        LocalDate paidDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal remainingAmount,
        BigDecimal lateFee,
        String status

) {
}
