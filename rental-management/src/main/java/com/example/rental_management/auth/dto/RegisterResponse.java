package com.example.rental_management.auth.dto;

import com.example.rental_management.user.entity.Role;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        String status
) {
}
