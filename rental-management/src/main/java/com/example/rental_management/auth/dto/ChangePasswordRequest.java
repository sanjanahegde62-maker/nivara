package com.example.rental_management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/change-password.
 * Passwords must not travel as query parameters.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, message = "newPassword must be at least 8 characters")
        String newPassword
) {
}
