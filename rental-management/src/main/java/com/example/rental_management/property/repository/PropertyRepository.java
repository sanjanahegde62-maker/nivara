package com.example.rental_management.property.repository;

import com.example.rental_management.property.entity.Property;
import com.example.rental_management.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property,Long> {
    List<Property> findByOwnerId(Long ownerId);
    Optional<Property> findByIdAndOwnerId(Long id,Long ownerId);



}
