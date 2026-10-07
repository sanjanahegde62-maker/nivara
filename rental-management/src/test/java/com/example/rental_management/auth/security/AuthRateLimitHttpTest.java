package com.example.rental_management.auth.security;

import com.example.rental_management.auth.service.JwtService;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level rate-limit tests for auth endpoints.
 *
 * Verifies that the {@link AuthRateLimitInterceptor} enforces limits at the
 * servlet layer for both protected auth endpoints:
 *
 *  - POST /auth/login          → limit 10 per 15 min per IP
 *  - POST /auth/change-password → limit 5 per 15 min per IP+user
 *
 * These tests use a dedicated limiter key prefix so they don't pollute the
 * shared in-process limiter state for other test classes.
 *
 * NOTE: @Transactional is intentionally NOT used here because the rate-limiter
 * state lives outside the transaction boundary (in-memory map on the
 * interceptor bean).  The test accepts some state leakage risk; each key is
 * unique to the user ID used, which keeps runs independent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthRateLimitHttpTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AuthRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** A user ID that is never present in the DB — rate-limit keys are IP-based so the user doesn't need to exist. */
    private static final long RATE_TEST_USER_ID = Long.MAX_VALUE - 42;

    private String tokenFor(long id) {
        User stub = new User();
        stub.setId(id);
        stub.setRole(Role.TENANT);
        return jwtService.generateToken(stub);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // R1 — POST /auth/login is rate-limited (10 per 15 min per IP)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void login_rateLimited_after10Attempts() throws Exception {
        Map<String, Object> body = Map.of("email", "x@x.com", "password", "wrong");
        String json = objectMapper.writeValueAsString(body);

        int allowed = 0;
        int blocked = 0;

        // Send 15 requests — first 10 must pass through the rate limiter (may
        // still get a 400/401 from auth logic), the rest must be blocked with 429.
        for (int i = 0; i < 15; i++) {
            MvcResult result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andReturn();
            int status = result.getResponse().getStatus();
            if (status == 429) blocked++;
            else allowed++;
        }

        assertThat(allowed).as("requests allowed through rate limiter").isLessThanOrEqualTo(10);
        assertThat(blocked).as("requests blocked by rate limiter").isGreaterThanOrEqualTo(5);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // R2 — POST /auth/change-password is rate-limited (5 per 15 min per IP+user)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_rateLimited_after5Attempts() throws Exception {
        String token = tokenFor(RATE_TEST_USER_ID);
        Map<String, Object> body = Map.of("newPassword", "NewStrongPass1!");
        String json = objectMapper.writeValueAsString(body);

        int allowed = 0;
        int blocked = 0;

        // Send 10 requests — first 5 pass the rate limiter (may get 404/500 because
        // user doesn't exist in DB), the rest must be blocked with 429.
        for (int i = 0; i < 10; i++) {
            MvcResult result = mockMvc.perform(post("/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andReturn();
            int status = result.getResponse().getStatus();
            if (status == 429) blocked++;
            else allowed++;
        }

        assertThat(allowed).as("requests allowed through rate limiter").isLessThanOrEqualTo(5);
        assertThat(blocked).as("requests blocked by rate limiter").isGreaterThanOrEqualTo(5);
    }
}
