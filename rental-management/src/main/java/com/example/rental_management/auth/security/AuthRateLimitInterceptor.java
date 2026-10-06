package com.example.rental_management.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final AuthRateLimiter limiter;

    public AuthRateLimitInterceptor(AuthRateLimiter limiter) { this.limiter = limiter; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        int limit;
        String identity;
        if ("/auth/login".equals(path)) {
            limit = 10;
            identity = request.getRemoteAddr();
        } else if ("/auth/change-password".equals(path)) {
            limit = 5;
            identity = request.getRemoteAddr() + ":" + request.getUserPrincipal().getName();
        } else {
            return true;
        }
        if (limiter.allow(path + ":" + identity, limit, WINDOW, System.currentTimeMillis())) return true;
        response.setStatus(429);
        response.setContentType("application/json");
        try { response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please try again later.\"}"); }
        catch (java.io.IOException ignored) { }
        return false;
    }
}
