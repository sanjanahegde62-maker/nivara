package com.example.rental_management.auth.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class TestController {
    @GetMapping("/test/auth")
    public String testAuthentication(Authentication authentication){
        Long userId= SecurityUtil.getCurrentUserId();
        return "authenticated user ID: "+userId+",role: "+authentication.getAuthorities();


    }
}
