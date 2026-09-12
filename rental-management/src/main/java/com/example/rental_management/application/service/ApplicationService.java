package com.example.rental_management.application.service;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.repository.UnitRepository;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final PropertyManagerRepository propertyManagerRepository;
    public ApplicationService(ApplicationRepository applicationRepository, UnitRepository unitRepository, UserRepository userRepository, PropertyManagerRepository propertyManagerRepository) {
        this.applicationRepository = applicationRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.propertyManagerRepository = propertyManagerRepository;
}
public Application createApplication(Long unitId, Long tenantId){
        Unit unit=unitRepository.findById(unitId)
                .orElseThrow(()->new RuntimeException("unit not found"));
        if(!"AVAILABLE".equals(unit.getStatus())){
            throw new IllegalStateException("unit is not available for application");
        }

        User tenant=userRepository.findById(tenantId)
            .orElseThrow(()->new RuntimeException("tenant not found"));
    if (applicationRepository.existsByUnitIdAndTenantId(unitId, tenantId)) {
        throw new IllegalStateException("you have already applied for this unit");
    }
        Application application=new Application();
        application.setUnit(unit);
        application.setTenant(tenant);
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }
    public List<Application> getMyApplications(Long tenantId) {
        return applicationRepository.findByTenantId(tenantId);
    }
    @Transactional
    public Application reviewApplicaton(
            Long applicationId,
            Long reviewerId,
            String decision) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("application not found"));

        Unit unit = application.getUnit();

        Long ownerId = unit.getProperty().getOwner().getId();

        boolean isOwner = ownerId.equals(reviewerId);

        if (!isOwner) {
            throw new RuntimeException(
                    "user not authorized to review this application"
            );
        }

        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException(
                    "decision must be approved or rejected"
            );
        }

        if (!"PENDING".equals(application.getStatus())) {
            throw new IllegalStateException(
                    "application has already been reviewed"
            );
        }

        application.setStatus(decision);
        application.setReviewedAt(LocalDateTime.now());

        if ("APPROVED".equals(decision)) {
            unit.setStatus("OCCUPIED");
            System.out.println("UNIT STATUS AFTER APPROVAL = " + unit.getStatus());
        }

        return applicationRepository.save(application);
    }
    public List<Application> getMyPropertyApplications(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));

        if ("OWNER".equals(user.getRole().name())) {
            return applicationRepository.findByUnitPropertyOwnerId(userId);
        }

        if ("MANAGER".equals(user.getRole().name())) {

            if (propertyManagerRepository.findByManagerId(userId).isEmpty()) {
                throw new RuntimeException("manager is not assigned to any property");
            }

            return applicationRepository.findByManagerId(userId);
        }

        throw new RuntimeException("user not authorized");
    }
}
