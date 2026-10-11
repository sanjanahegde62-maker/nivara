package com.example.rental_management.user.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.user.dto.UserSummaryResponse;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * User directory endpoints.
 *
 * GET /users?role=MANAGER — list all active users with a given role.
 * Accessible by OWNER only; used by the frontend manager-assignment UI.
 * Never exposes passwordHash, token, or createdAt/updatedAt.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns a safe summary list of users filtered by role.
     * Only OWNER may call this endpoint (used for manager assignment).
     *
     * @param role the role to filter by (e.g. "MANAGER")
     */
    @GetMapping
    public List<UserSummaryResponse> listByRole(@RequestParam String role) {
        if (SecurityUtil.getCurrentUserRole() != Role.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only owners can browse the user directory");
        }
        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + role);
        }
        return userRepository.findByRole(parsedRole)
                .stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name()))
                .toList();
    }
}
