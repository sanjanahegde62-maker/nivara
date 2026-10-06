package com.example.rental_management.auth.controller;

import com.example.rental_management.auth.dto.ChangePasswordRequest;
import com.example.rental_management.auth.dto.LoginRequest;
import com.example.rental_management.auth.dto.LoginResponse;
import com.example.rental_management.auth.dto.RegisterRequest;
import com.example.rental_management.auth.dto.RegisterResponse;
import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.auth.service.AuthService;
import com.example.rental_management.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.name(),
                request.email(),
                request.password(),
                request.phone(),
                request.role());
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        String token = authService.generateToken(user);
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token);
    }

    /**
     * Changes the password of the currently authenticated user.
     *
     * <p>The target user is always derived from the JWT — clients cannot change
     * another user's password by supplying a different email or ID.
     * The new password travels in the request body, never as a query parameter.
     */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        authService.changePassword(currentUserId, request.newPassword());
    }
}
