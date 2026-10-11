package com.example.rental_management.user.dto;

/**
 * Safe, minimal user summary.
 * Never exposes passwordHash, token, or internal timestamps.
 * Used for manager-directory lookups and assignment UIs.
 */
public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        String role
) {}
