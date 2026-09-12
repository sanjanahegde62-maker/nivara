package com.example.rental_management.property.repository;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.property.entity.PropertyManager;
import com.example.rental_management.property.entity.PropertyManagerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PropertyManagerRepository
        extends JpaRepository<PropertyManager, PropertyManagerId> {

    Optional<PropertyManager> findByPropertyIdAndManagerId(
            Long propertyId,
            Long managerId
    );
    List<PropertyManager> findByManagerId(Long managerId);
    void deleteByPropertyIdAndManagerId(Long propertyId, Long managerId);

}