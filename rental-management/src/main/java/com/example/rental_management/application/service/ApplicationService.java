package com.example.rental_management.application.service;

import com.example.rental_management.application.dto.ApplicationResponse;
import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.repository.UnitRepository;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            UnitRepository unitRepository,
            UserRepository userRepository,
            PropertyManagerRepository propertyManagerRepository) {
        this.applicationRepository = applicationRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    public ApplicationResponse createApplication(Long unitId, Long tenantId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("unit not found"));

        if (!"AVAILABLE".equals(unit.getStatus())) {
            throw new IllegalStateException("unit is not available for application");
        }

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("tenant not found"));

        if (applicationRepository.existsByUnitIdAndTenantId(unitId, tenantId)) {
            throw new IllegalStateException("you have already applied for this unit");
        }

        Application application = new Application();
        application.setUnit(unit);
        application.setTenant(tenant);
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    public List<ApplicationResponse> getMyApplications(Long tenantId) {
        return applicationRepository.findByTenantId(tenantId)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ApplicationResponse reviewApplication(
            Long applicationId, Long reviewerId, String decision) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("application not found"));

        Unit unit = application.getUnit();
        Long propertyId = unit.getProperty().getId();
        Long ownerId = unit.getProperty().getOwner().getId();

        // Authorise: OWNER of the property OR MANAGER assigned to the property
        boolean isOwner = ownerId.equals(reviewerId);
        boolean isManager = propertyManagerRepository
                .findByPropertyIdAndManagerId(propertyId, reviewerId)
                .isPresent();

        if (!isOwner && !isManager) {
            throw new RuntimeException("user not authorized to review this application");
        }

        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }

        if (!"PENDING".equals(application.getStatus())) {
            throw new IllegalStateException("application has already been reviewed");
        }

        application.setStatus(decision);
        application.setReviewedAt(LocalDateTime.now());

        if ("APPROVED".equals(decision)) {
            unit.setStatus("OCCUPIED");
        }

        return ApplicationResponse.from(applicationRepository.save(application));
    }

    public List<ApplicationResponse> getMyPropertyApplications(Long userId) {
        // Use the security context for role — avoids an unnecessary DB lookup
        // and ensures the caller cannot impersonate a different user's role.
        Role role = SecurityUtil.getCurrentUserRole();

        if (role == Role.OWNER) {
            return applicationRepository.findByUnitPropertyOwnerId(userId)
                    .stream().map(ApplicationResponse::from).toList();
        }

        if (role == Role.MANAGER) {
            return applicationRepository.findByManagerId(userId)
                    .stream().map(ApplicationResponse::from).toList();
        }

        throw new RuntimeException("user not authorized");
    }
}
