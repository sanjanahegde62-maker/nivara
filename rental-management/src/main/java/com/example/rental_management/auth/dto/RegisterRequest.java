package com.example.rental_management.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.rental_management.user.entity.Role;

public record RegisterRequest (
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 8)
        String password,
        String phone,
        @NotNull
        Role role

){
}
