package com.example.rental_management.lease.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.service.LeaseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leases")

public class LeaseController {
    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping("/applications/{applicationId}")
    public Lease createLease(@PathVariable Long applicationId, @RequestParam
                             @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
                             LocalDate startDate, @RequestParam
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             LocalDate endDate) {
        return leaseService.createLease(applicationId, startDate, endDate);
    }
    @GetMapping("/my")
    public List<Lease> getMyLeases(){
        Long tenantId= SecurityUtil.getCurrentUserId();
        return leaseService.getMyLeases(tenantId);
    }
    @GetMapping("/{leaseId}")
    public Lease getLeaseById(@PathVariable Long leaseId){
        Long tenantId=SecurityUtil.getCurrentUserId();
        return leaseService.getLeaseById(leaseId,tenantId);
    }
    @GetMapping("/property")
    public List<Lease> getPropertyLeases() {

        Long ownerId = SecurityUtil.getCurrentUserId();

        return leaseService.getPropertyLeases(ownerId);
    }
}
