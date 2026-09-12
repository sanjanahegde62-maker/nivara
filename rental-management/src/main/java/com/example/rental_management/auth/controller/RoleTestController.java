package com.example.rental_management.auth.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleTestController {
    @GetMapping("/test/owner")
    @PreAuthorize("hasRole('OWNER')")
    public String ownerOnly(){
        return "OWNER access granted";
    }
    @GetMapping("/test/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public String tenantOnly(){
        return "TENANT access granted";
    }

    @GetMapping("/test/maintenance")
    @PreAuthorize("hasRole('MAINTENANCE_STAFF')")
    public String maintenanceOnly(){
        return "MAINTENANCE_STAFF access granted";
    }

}
