package com.example.rental_management.lease.repository;

import com.example.rental_management.lease.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    Optional<Lease> findByApplicationId(Long applicationId);

    List<Lease> findByTenantId(Long tenantId);

    @Query("""
        SELECT l
        FROM Lease l
        JOIN l.unit u
        JOIN u.property p
        WHERE p.owner.id = :ownerId
    """)
    List<Lease> findByPropertyOwnerId(@Param("ownerId") Long ownerId);
}