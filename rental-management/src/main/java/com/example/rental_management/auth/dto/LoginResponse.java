package com.example.rental_management.auth.dto;

import com.example.rental_management.user.entity.Role;

public record LoginResponse(
        Long id,
        String name,
        String email,
        Role role,
        String token
) {
}
