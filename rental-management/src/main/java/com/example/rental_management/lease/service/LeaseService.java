package com.example.rental_management.lease.service;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.property.entity.Unit;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaseService {
    private final LeaseRepository leaseRepository;
    private final ApplicationRepository applicationRepository;

    public LeaseService(LeaseRepository leaseRepository, ApplicationRepository applicationRepository) {
        this.leaseRepository = leaseRepository;
        this.applicationRepository = applicationRepository;
    }
    public Lease createLease(Long applicationId, LocalDate startDate, LocalDate endDate){
        Application application=applicationRepository.findById(applicationId)
                .orElseThrow(()-> new RuntimeException("application not found"));
        if(!"APPROVED".equals(application.getStatus())){
            throw new IllegalStateException("lease can be only created for an approved application");

        }
        if(leaseRepository.findByApplicationId(applicationId).isPresent()){
            throw new IllegalStateException("lease already exists for this application");

        }
        Unit unit=application.getUnit();
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
    public List<Lease> getMyLeases(Long tenantId){
        return leaseRepository.findByTenantId(tenantId);
    }
    public Lease getLeaseById(Long leaseId,Long tenantId){
        Lease lease=leaseRepository.findById(leaseId)
                .orElseThrow(()->new RuntimeException("lease not found"));
        if(!lease.getTenant().getId().equals(tenantId)){
            throw new RuntimeException("user not authorized to view this lease");
        }
        return lease;
    }
    public List<Lease> getPropertyLeases(Long ownerId) {
        return leaseRepository.findByPropertyOwnerId(ownerId);
    }
}
