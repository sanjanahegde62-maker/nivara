package com.example.rental_management.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    private SecurityUtil(){

    }
    public static Long getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       if(authentication == null || !authentication.isAuthenticated()) {
           throw new IllegalStateException("no authenticated user found");

       }
       return Long.parseLong(authentication.getName());
    }
    public static String getCurrentUserRole(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()){
            throw new IllegalStateException("no authenticated user found");
        }

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("no role found for authenticated user"))
                .getAuthority();



        return role.replace("ROLE_","");
    }
}
