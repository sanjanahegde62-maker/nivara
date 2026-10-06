package com.example.rental_management.auth.service;

import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.rental_management.user.entity.Role;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    public User register(String name, String email, String password, String phone, @NotNull Role role){
        if(userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus("ACTIVE");

        LocalDateTime now= LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    public User login(String email,String password){
        User user=userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid email or password"));
                if(!passwordEncoder.matches(password,user.getPasswordHash())){
                    throw new IllegalArgumentException("invalid email or password");
                }
                if(!"ACTIVE".equals(user.getStatus())){
                    throw new IllegalArgumentException("user account is not active");
                }
                return user;
    }

    public String generateToken(User user) {
        return jwtService.generateToken(user);
    }
    /**
     * Changes the password for the currently authenticated user only.
     * The caller supplies their own user ID (derived from the JWT); they cannot
     * change another user's password.  The new password is BCrypt-hashed before
     * persistence and is never logged.
     */
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }
}
