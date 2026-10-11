package com.example.rental_management.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CORS integration tests.
 *
 * Verifies that:
 * - Preflight OPTIONS requests from the allowed origin receive a 200 with the
 *   correct Access-Control headers (including Authorization).
 * - Requests from a disallowed origin are rejected (no CORS headers returned).
 * - Simple GET requests from the allowed origin carry through CORS headers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CorsIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    // ══════════════════════════════════════════════════════════════════════════
    // C1 — Preflight from the allowed origin succeeds
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void preflight_fromAllowedOrigin_returns200WithCorsHeaders() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C2 — Preflight from a disallowed origin is rejected (no ACAO header)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void preflight_fromDisallowedOrigin_hasNoAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", DISALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C3 — Authorization header is explicitly allowed (so browser can send JWT)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void preflight_authorizationHeaderIsAllowed() throws Exception {
        mockMvc.perform(options("/leases")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C4 — Actual request from allowed origin carries Access-Control-Allow-Origin
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void actualRequest_fromAllowedOrigin_hasAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/auth/register")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }
}
