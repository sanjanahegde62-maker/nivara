package com.example.rental_management.property.dto;

import java.math.BigDecimal;

public record UnitRequest(
        String unitNumber,
        String unitType,
        BigDecimal monthlyRent,
        String status
) {
}
