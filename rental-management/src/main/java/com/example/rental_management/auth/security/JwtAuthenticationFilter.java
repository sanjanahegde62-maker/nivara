package com.example.rental_management.auth.security;

import com.example.rental_management.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("========== JWT FILTER CALLED ==========");


        String authHeader = request.getHeader("Authorization");
        System.out.println("AUTH HEADER = " + authHeader);
        System.out.println("REQUEST = " + request.getRequestURI());

        if (authHeader == null ||
                !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        System.out.println("TOKEN VALID = " + jwtService.isTokenValid(token));
        if (!jwtService.isTokenValid(token)) {

            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        System.out.println("USER ID = " + userId);
        System.out.println("ROLE = " + role);

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        System.out.println("JWT USER = " + userId);
        System.out.println("JWT ROLE = " + role);

        filterChain.doFilter(request, response);
    }
}