package com.example.rental_management.property.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed property view — returned by GET /properties/{id} when the caller
 * explicitly requests the detail view (or always).
 *
 * Includes accurate unit statistics derived from real unit records,
 * and the list of assigned managers.
 */
public record PropertyDetailResponse(
        Long id,
        String name,
        String address,
        String propertyType,
        String description,
        int totalUnits,
        int occupiedUnits,
        int vacantUnits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<UnitResponse> units,
        List<ManagerAssignmentResponse> managers
) {}
