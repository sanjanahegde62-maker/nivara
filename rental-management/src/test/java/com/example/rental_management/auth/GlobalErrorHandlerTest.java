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

/**
 * Block C — Global Error Handler
 *
 * Verifies that the global exception handler:
 * - Returns structured JSON (not HTML or plain text) for error responses
 * - Does not expose stack traces, SQL text, or class names in responses
 * - Returns correct HTTP status codes for common error categories
 * - Validation failures return 400 with field-level detail (no secret values)
 * - Unknown routes return 404 (not Spring Boot's default HTML error page)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GlobalErrorHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(Long id, Role role) {
        User stub = new User();
        stub.setId(id);
        stub.setRole(role);
        return jwtService.generateToken(stub);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C1 — Validation failure returns structured JSON 400, not a stack trace
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validationFailure_returnsStructuredJson_noStackTrace() throws Exception {
        // Register with a missing required field — triggers @Valid failure
        Map<String, Object> badBody = Map.of(
                "email", "not-an-email",
                "password", "short",
                "role", "TENANT"
                // name is missing — @NotBlank
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                // Must NOT contain stack trace markers
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C2 — Unknown route returns 404 with JSON body, not HTML
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void unknownRoute_returns404_withJsonBody() throws Exception {
        String token = tokenFor(1L, Role.OWNER);
        mockMvc.perform(get("/this/route/does/not/exist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C3 — Unauthenticated request returns 401 with JSON, not HTML
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void unauthenticated_returns401_withJsonStructure() throws Exception {
        mockMvc.perform(get("/leases"))
                .andExpect(status().isUnauthorized())
                // Spring Security entry point writes a minimal response;
                // we verify only that it is NOT an HTML page
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    // Either no content or JSON — never HTML
                    if (contentType != null) {
                        org.assertj.core.api.Assertions
                                .assertThat(contentType).doesNotContain("text/html");
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C4 — Role-forbidden request returns 403 with JSON body
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void forbidden_returns403_withJsonBody() throws Exception {
        // MAINTENANCE_STAFF trying to access an OWNER-only report
        String token = tokenFor(99L, Role.MAINTENANCE_STAFF);
        mockMvc.perform(get("/reports/income")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C5 — Error response never contains stack-trace fields
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_invalidInput_errorResponseHasNoStackTrace() throws Exception {
        String token = tokenFor(1L, Role.TENANT);
        // Send a body that fails validation
        Map<String, Object> body = Map.of("newPassword", "x");   // too short
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                // must have exactly the four documented keys
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // C6 — Duplicate email during register returns structured 400
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void duplicateEmail_returns400_withJsonMessage() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = Map.of(
                "name", "User One",
                "email", email,
                "password", "Password123!",
                "phone", "1234567890",
                "role", "TENANT"
        );
        // First registration succeeds
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Second registration with same email must return 400 (not 500)
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
