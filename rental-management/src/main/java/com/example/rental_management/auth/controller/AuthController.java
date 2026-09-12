package com.example.rental_management.auth.controller;

import com.example.rental_management.auth.dto.LoginRequest;
import com.example.rental_management.auth.dto.LoginResponse;
import com.example.rental_management.auth.dto.RegisterRequest;
import com.example.rental_management.auth.dto.RegisterResponse;
import com.example.rental_management.auth.service.AuthService;
import com.example.rental_management.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.example.rental_management.user.entity.User;
@RestController
@RequestMapping("/auth")

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request){
        User user = authService.register(request.name(),
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
                user.getStatus()
        );
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        User user = authService.login(
                request.email(),
                request.password()
        );
        String token=authService.generateToken(user);
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {

        authService.resetPassword(email, newPassword);

        return "Password reset successfully";
    }
}
