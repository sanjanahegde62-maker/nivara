package com.example.rental_management.maintenance.repository;

import com.example.rental_management.maintenance.entity.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest,Long> {
    List<MaintenanceRequest> findByTenantId(Long tenantId);
    List<MaintenanceRequest> findByUnitId(Long unitId);
    List<MaintenanceRequest> findByAssignedStaffId(Long staffId);
    List<MaintenanceRequest> findByStatus(String status);
    List<MaintenanceRequest> findByUnitPropertyOwnerId(Long ownerId);
    @Query("""
    SELECT mr
    FROM MaintenanceRequest mr
    JOIN mr.unit u
    JOIN u.property p
    JOIN PropertyManager pm ON pm.property.id = p.id
    WHERE pm.manager.id = :managerId
""")
    List<MaintenanceRequest> findByManagerId(@Param("managerId") Long managerId);
    Optional<MaintenanceRequest> findByIdAndTenantId(Long id,Long tenantId);
    Optional<MaintenanceRequest> findByIdAndUnitPropertyOwnerId(Long id,Long ownerId);

    Optional<MaintenanceRequest> findByIdAndAssignedStaffId(Long id,Long staffId);
    @Query("""
    SELECT mr
    FROM MaintenanceRequest mr
    JOIN mr.unit u
    JOIN u.property p
    JOIN PropertyManager pm ON pm.property.id = p.id
    WHERE mr.id = :requestId
    AND pm.manager.id = :managerId
""")
    Optional<MaintenanceRequest> findByIdAndManagerId(
            @Param("requestId") Long requestId,
            @Param("managerId") Long managerId
    );
}
