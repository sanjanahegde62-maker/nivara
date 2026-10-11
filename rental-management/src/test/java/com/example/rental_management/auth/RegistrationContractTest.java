package com.example.rental_management.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Block R — Registration contract tests
 *
 * Verifies that POST /auth/register:
 * - Returns HTTP 201 with a JWT token in the response body (so the frontend
 *   can log the user in immediately after registration without a second call)
 * - Returns 400 for duplicate email (via IllegalArgumentException → handler)
 * - Returns 400 for invalid credentials on login (not 500)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RegistrationContractTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ══════════════════════════════════════════════════════════════════════════
    // R1 — Register returns 201 with token in body
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void register_returnsCreated_withTokenInBody() throws Exception {
        String email = "reg-contract-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = Map.of(
                "name", "New User",
                "email", email,
                "password", "SecurePass1!",
                "role", "TENANT"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("TENANT"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // R2 — Duplicate email returns 400 (IllegalArgumentException → handler)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        String email = "reg-dup-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = Map.of(
                "name", "First User",
                "email", email,
                "password", "SecurePass1!",
                "role", "OWNER"
        );
        // First call succeeds
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Second call with same email → 400
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // R3 — Invalid login credentials return 400 (not 500)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "nobody-" + UUID.randomUUID() + "@example.com",
                "password", "wrongpassword"
        );
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // R4 — Successful login returns token
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        String email = "login-ok-" + UUID.randomUUID() + "@example.com";
        // Register first
        Map<String, Object> reg = Map.of(
                "name", "Login Test",
                "email", email,
                "password", "SecurePass1!",
                "role", "TENANT"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login
        Map<String, Object> login = Map.of("email", email, "password", "SecurePass1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("TENANT"));
    }
}
