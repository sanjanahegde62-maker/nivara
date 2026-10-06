package com.example.rental_management.auth;

import com.example.rental_management.auth.service.AuthService;
import com.example.rental_management.auth.service.JwtService;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Block B — Password Change Authorization
 *
 * Tests the /auth/change-password endpoint:
 * - Unauthenticated requests are rejected (401)
 * - Authenticated user can change their own password
 * - Authenticated user CANNOT change another user's password
 * - Changed password is BCrypt-hashed, not stored in plaintext
 * - Invalid input (blank/short password) returns 400
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PasswordChangeTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String emailSuffix, Role role) {
        User u = new User();
        u.setName("Test " + emailSuffix);
        u.setEmail("pwtest-" + emailSuffix + "-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash(passwordEncoder.encode("OriginalPass1!"));
        u.setRole(role);
        u.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B1 — No JWT → 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_withoutJwt_returns401() throws Exception {
        Map<String, Object> body = Map.of("newPassword", "NewSecurePass1!");
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B2 — Authenticated user can change their own password → 204
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_ownAccount_succeeds() throws Exception {
        User owner = createUser("owner", Role.OWNER);
        String token = tokenFor(owner);

        Map<String, Object> body = Map.of("newPassword", "ChangedPass1!");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B3 — New password is BCrypt-hashed, not stored in plaintext
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_newPasswordIsHashed_notPlaintext() throws Exception {
        User tenant = createUser("tenant", Role.TENANT);
        String token = tokenFor(tenant);
        String newPassword = "HashedPass9#";

        Map<String, Object> body = Map.of("newPassword", newPassword);
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(tenant.getId()).orElseThrow();

        // The stored value must not equal the plaintext password
        assertThat(updated.getPasswordHash()).isNotEqualTo(newPassword);

        // The stored value must be a valid BCrypt hash that matches the new password
        assertThat(passwordEncoder.matches(newPassword, updated.getPasswordHash())).isTrue();

        // The old password must no longer match
        assertThat(passwordEncoder.matches("OriginalPass1!", updated.getPasswordHash())).isFalse();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B4 — User A cannot change User B's password
    //      The endpoint derives the target user from the JWT — there is no
    //      way to specify a different target.  A's token always targets A.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_cannotTargetAnotherUser() throws Exception {
        User userA = createUser("a", Role.TENANT);
        User userB = createUser("b", Role.TENANT);
        String tokenForA = tokenFor(userA);

        String newPassword = "AttackerNewPass1!";
        Map<String, Object> body = Map.of("newPassword", newPassword);

        // userA authenticates and calls change-password
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + tokenForA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        // userB's password must NOT have changed
        User refreshedB = userRepository.findById(userB.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, refreshedB.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("OriginalPass1!", refreshedB.getPasswordHash())).isTrue();

        // userA's password MUST have changed (confirms the call targeted A)
        User refreshedA = userRepository.findById(userA.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, refreshedA.getPasswordHash())).isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B5 — Blank new password → 400
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_blankNewPassword_returns400() throws Exception {
        User user = createUser("blank", Role.TENANT);
        String token = tokenFor(user);

        Map<String, Object> body = Map.of("newPassword", "");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // B6 — Too-short new password → 400
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_tooShortNewPassword_returns400() throws Exception {
        User user = createUser("short", Role.TENANT);
        String token = tokenFor(user);

        Map<String, Object> body = Map.of("newPassword", "abc");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
