package com.example.rental_management.property.repository;

import com.example.rental_management.property.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit,Long> {
    List<Unit> findByPropertyId(Long propertyId);
    Optional<Unit> findByIdAndPropertyId(Long id, Long propertyId);

}
