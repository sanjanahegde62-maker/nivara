package com.example.rental_management.auth;

import com.example.rental_management.auth.service.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Block D — JWT Configuration Startup Validation
 *
 * Verifies that JwtService fails fast with a clear, safe message when
 * misconfigured.  Tests instantiate JwtService directly (no Spring context)
 * to exercise the constructor validation in isolation.
 *
 * None of the error messages must contain the secret value itself.
 */
class JwtConfigValidationTest {

    private static final String VALID_SECRET =
            "test-only-secret-key-minimum-32-chars-for-hmac-sha256";
    private static final long VALID_EXPIRATION = 3_600_000L;

    // ══════════════════════════════════════════════════════════════════════════
    // D1 — Valid configuration starts successfully
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validConfig_startsSuccessfully() {
        assertThatNoException()
                .isThrownBy(() -> new JwtService(VALID_SECRET, VALID_EXPIRATION));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D2 — Empty secret fails startup with a clear message
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void emptySecret_throwsIllegalStateException_withClearMessage() {
        assertThatThrownBy(() -> new JwtService("", VALID_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret")
                .hasMessageContaining("JWT_SECRET");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D3 — Blank (whitespace) secret fails startup
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void blankSecret_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService("   ", VALID_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D4 — Secret shorter than 32 bytes fails startup with a clear message
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void tooShortSecret_throwsIllegalStateException_withClearMessage() {
        String shortSecret = "only-31-characters-long-here!!";  // 30 chars
        assertThatThrownBy(() -> new JwtService(shortSecret, VALID_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret")
                .hasMessageContaining("32")
                // Secret value must NOT appear in the exception message
                .hasMessageNotContaining(shortSecret);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D5 — Zero expiration fails startup
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void zeroExpiration_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService(VALID_SECRET, 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.expiration")
                .hasMessageContaining("JWT_EXPIRATION");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // D6 — Negative expiration fails startup
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void negativeExpiration_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtService(VALID_SECRET, -1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.expiration");
    }
}
