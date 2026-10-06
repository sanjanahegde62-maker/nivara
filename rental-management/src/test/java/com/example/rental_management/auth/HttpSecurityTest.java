package com.example.rental_management.auth;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;


/**
 * Block A — Authentication & HTTP Security
 *
 * Tests the actual HTTP security boundary via MockMvc:
 * - Public endpoints accessible without JWT
 * - Protected endpoints require JWT
 * - Malformed/invalid/tampered JWT returns 401
 * - /auth/reset-password is NOT publicly accessible (returns 401 without JWT)
 * - Valid JWT authenticates successfully
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class HttpSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    // ObjectMapper is not a Spring bean in the full-boot MOCK context — use a plain instance
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Create a valid JWT for an in-memory user stub. */
    private String validJwt(Long id, Role role) {
        User stub = new User();
        stub.setId(id);
        stub.setRole(role);
        return jwtService.generateToken(stub);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A1 — POST /auth/register is accessible without JWT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void register_isPublic_noJwtRequired() throws Exception {
        String email = "sec-test-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = Map.of(
                "name", "Test User",
                "email", email,
                "password", "Password123!",
                "phone", "9999999999",
                "role", "TENANT"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A2 — POST /auth/login is accessible without JWT (bad creds → 400/401 from
    //      auth logic, NOT a 401 from the JWT filter)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void login_isPublic_noJwtRequired() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "nosuchuser@example.com",
                "password", "wrong"
        );
        // The endpoint is reachable — we just verify the request is NOT rejected
        // with 401 by the JWT filter (which would happen before the controller).
        // Auth logic may return 400 for invalid credentials; that is acceptable.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(not(equalTo(401))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A3 — Protected endpoint without JWT returns 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void protectedEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/leases"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A4 — Protected endpoint with malformed JWT returns 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void protectedEndpoint_withMalformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/leases")
                        .header("Authorization", "Bearer this.is.not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A5 — Protected endpoint with tampered/invalid JWT returns 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void protectedEndpoint_withTamperedJwt_returns401() throws Exception {
        String jwt = validJwt(1L, Role.TENANT);
        // Tamper: flip the last character of the signature
        String tampered = jwt.substring(0, jwt.length() - 1) + "X";
        mockMvc.perform(get("/leases")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A6 — Valid JWT authenticates the request (passes the JWT filter)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void protectedEndpoint_withValidJwt_isAuthenticated() throws Exception {
        String jwt = validJwt(1L, Role.TENANT);
        // With a valid JWT the filter passes the request to the controller.
        // The controller may return 200 or a domain error — but NOT 401.
        mockMvc.perform(get("/leases")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().is(not(equalTo(401))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A7 — /auth/change-password is NOT publicly accessible (requires JWT)
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
    // A8 — Multiple protected endpoints all require JWT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void multipleProtectedEndpoints_withoutJwt_allReturn401() throws Exception {
        mockMvc.perform(get("/leases/1/charges")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/reports/income")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/leases/1/document")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/maintenance-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // A9 — JWT signed with a different key is rejected
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void protectedEndpoint_withWrongSignatureJwt_returns401() throws Exception {
        // A real JWT structure but signed with a different secret
        String foreignJwt = "eyJhbGciOiJIUzI1NiJ9." +
                "eyJzdWIiOiIxIiwicm9sZSI6IlRFTkFOVCJ9." +
                "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        mockMvc.perform(get("/leases")
                        .header("Authorization", "Bearer " + foreignJwt))
                .andExpect(status().isUnauthorized());
    }
}
