package com.example.rental_management.property.dto;

import java.time.LocalDateTime;

public record PropertyResponse(
        Long id,

        String name,
        String address,
        String propertyType,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
