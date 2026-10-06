package com.example.rental_management.auth.service;

import com.example.rental_management.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    /** HMAC-SHA256 requires at least 32 bytes (256 bits). */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        // ── Startup validation ────────────────────────────────────────────────
        // Fail fast with a clear message rather than producing a cryptographic
        // exception later during the first authentication attempt.
        // The secret VALUE is never logged or included in any exception message.
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT configuration error: jwt.secret (JWT_SECRET) must not be empty. " +
                    "Set the JWT_SECRET environment variable to a random string of at " +
                    "least " + MIN_SECRET_BYTES + " characters.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT configuration error: jwt.secret (JWT_SECRET) is too short. " +
                    "HMAC-SHA256 requires a key of at least " + MIN_SECRET_BYTES +
                    " bytes (" + MIN_SECRET_BYTES + " ASCII characters). " +
                    "Generate a stronger secret and set it via the JWT_SECRET environment variable.");
        }
        if (expiration <= 0) {
            throw new IllegalStateException(
                    "JWT configuration error: jwt.expiration (JWT_EXPIRATION) must be a " +
                    "positive number of milliseconds. Current value: " + expiration);
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    public String generateToken(User user) {
        Date now=new Date();
        Date expiry=new Date(now.getTime()+expiration);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role",user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

    }
    public String extractUserId(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    public boolean isTokenValid(String token){
        try{
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);

    }
}
