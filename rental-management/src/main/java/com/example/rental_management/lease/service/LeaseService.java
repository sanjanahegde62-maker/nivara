package com.example.rental_management.lease.service;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.user.entity.Role;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final ApplicationRepository applicationRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public LeaseService(
            LeaseRepository leaseRepository,
            ApplicationRepository applicationRepository,
            PropertyManagerRepository propertyManagerRepository) {
        this.leaseRepository = leaseRepository;
        this.applicationRepository = applicationRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    public Lease createLease(Long applicationId, LocalDate startDate, LocalDate endDate) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("application not found"));
        if (!"APPROVED".equals(application.getStatus())) {
            throw new IllegalStateException("lease can be only created for an approved application");
        }
        if (leaseRepository.findByApplicationId(applicationId).isPresent()) {
            throw new IllegalStateException("lease already exists for this application");
        }

        // Date validation
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        // Overlap prevention: reject if another ACTIVE lease already exists for this unit
        Unit unit = application.getUnit();
        leaseRepository.findActiveLeaseForUnit(unit.getId()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "unit already has an active lease (id=" + existing.getId() + ")");
        });

        Lease lease = new Lease();
        lease.setApplication(application);
        lease.setTenant(application.getTenant());
        lease.setUnit(unit);
        lease.setStartDate(startDate);
        lease.setEndDate(endDate);
        lease.setMonthlyRent(unit.getMonthlyRent());
        lease.setStatus("ACTIVE");
        return leaseRepository.save(lease);
    }

    /**
     * Role-aware lease list for GET /leases.
     * TENANT → their leases; OWNER → property leases; MANAGER → assigned property leases.
     */
    public List<Lease> getLeasesForCurrentUser(Long userId, Role role) {
        return switch (role) {
            case TENANT -> leaseRepository.findByTenantId(userId);
            case OWNER -> leaseRepository.findByPropertyOwnerId(userId);
            case MANAGER -> leaseRepository.findByManagerId(userId);
            case MAINTENANCE_STAFF -> throw new IllegalStateException(
                    "maintenance staff do not have access to lease information");
        };
    }

    /** Backward-compat: TENANT shortcut. */
    public List<Lease> getMyLeases(Long tenantId) {
        return leaseRepository.findByTenantId(tenantId);
    }

    /**
     * Get a single lease by ID with row-level auth.
     * TENANT may only see their own lease.
     * OWNER may only see leases for properties they own.
     * MANAGER may only see leases for properties assigned to them.
     */
    public Lease getLeaseById(Long leaseId, Long userId, Role role) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalStateException("lease not found"));

        boolean allowed = switch (role) {
            case TENANT -> lease.getTenant().getId().equals(userId);
            case OWNER -> lease.getUnit().getProperty().getOwner().getId().equals(userId);
            case MANAGER -> propertyManagerRepository
                    .findByPropertyIdAndManagerId(lease.getUnit().getProperty().getId(), userId)
                    .isPresent();
            case MAINTENANCE_STAFF -> false;
        };

        if (!allowed) {
            throw new IllegalStateException("user not authorized to view this lease");
        }
        return lease;
    }

    /** Backward-compat: OWNER property leases. */
    public List<Lease> getPropertyLeases(Long ownerId) {
        return leaseRepository.findByPropertyOwnerId(ownerId);
    }
}
