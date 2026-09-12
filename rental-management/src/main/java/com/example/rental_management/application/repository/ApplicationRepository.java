package com.example.rental_management.application.repository;

import com.example.rental_management.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByTenantId(Long tenantId);

    List<Application> findByUnitPropertyOwnerId(Long ownerId);
    boolean existsByUnitIdAndTenantId(Long unitId, Long tenantId);
    @Query("""
        SELECT a
        FROM Application a
        JOIN a.unit u
        JOIN PropertyManager pm ON pm.property.id = u.property.id
        WHERE pm.manager.id = :managerId
    """)
    List<Application> findByManagerId(@Param("managerId") Long managerId);
}