package com.example.rental_management.property.dto;

import java.math.BigDecimal;

public record UnitResponse(
        Long id,
        Long propertyId,
        String unitNumber,
        String unitType,
        BigDecimal monthlyRent,
        String status,
        String createdAt,
        String updatedAt
) {
}
