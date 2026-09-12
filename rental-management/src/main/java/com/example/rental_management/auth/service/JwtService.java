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
    private final SecretKey secretKey;
    private final long expiration;
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("604800000") long expiration){
        this.secretKey=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration=expiration;
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
