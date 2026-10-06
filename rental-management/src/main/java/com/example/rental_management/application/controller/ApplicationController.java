package com.example.rental_management.application.controller;

import com.example.rental_management.application.dto.ApplicationResponse;
import com.example.rental_management.application.service.ApplicationService;
import com.example.rental_management.auth.security.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Submit an application for a unit (TENANT only). */
    @PostMapping("/units/{unitId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(@PathVariable Long unitId) {
        Long tenantId = SecurityUtil.getCurrentUserId();
        return applicationService.createApplication(unitId, tenantId);
    }

    /**
     * Approve or reject a pending application.
     * Accessible by OWNER (of the property) and MANAGER (assigned to the property).
     */
    @PutMapping("/{applicationId}/review")
    public ApplicationResponse reviewApplication(
            @PathVariable Long applicationId,
            @RequestParam String decision) {
        Long reviewerId = SecurityUtil.getCurrentUserId();
        return applicationService.reviewApplication(applicationId, reviewerId, decision);
    }

    /** A tenant's view of their own applications. */
    @GetMapping("/my")
    public List<ApplicationResponse> getMyApplications() {
        Long tenantId = SecurityUtil.getCurrentUserId();
        return applicationService.getMyApplications(tenantId);
    }

    /** Owner/Manager view of applications for their properties. */
    @GetMapping("/property")
    public List<ApplicationResponse> getPropertyApplications() {
        Long userId = SecurityUtil.getCurrentUserId();
        return applicationService.getMyPropertyApplications(userId);
    }
}
