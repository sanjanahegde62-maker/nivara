package com.example.rental_management.application.controller;

import com.example.rental_management.application.entity.Application;
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
    @PostMapping("/units/{unitId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Application createApplication(@PathVariable Long unitId) {
        Long tenantId= SecurityUtil.getCurrentUserId();
        return applicationService.createApplication(unitId,tenantId);
    }
    @PutMapping("/{applicationId}/review")
    public Application reviewApplication(@PathVariable Long applicationId,@RequestParam String decision){
        Long reviewerId=SecurityUtil.getCurrentUserId();
        return applicationService.reviewApplicaton(applicationId,reviewerId,decision);
    }
    @GetMapping("/my")
    public List<Application> getMyApplications() {
        Long tenantId = SecurityUtil.getCurrentUserId();
        return applicationService.getMyApplications(tenantId);
    }
    @GetMapping("/property")
    public List<Application> getPropertyApplications() {
        Long ownerId = SecurityUtil.getCurrentUserId();
        return applicationService.getMyPropertyApplications(ownerId);
    }
}
