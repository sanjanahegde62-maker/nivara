package com.example.rental_management.auth.config;

import com.example.rental_management.auth.security.AuthRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthRateLimitConfig implements WebMvcConfigurer {
    private final AuthRateLimitInterceptor interceptor;
    public AuthRateLimitConfig(AuthRateLimitInterceptor interceptor) { this.interceptor = interceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
}
