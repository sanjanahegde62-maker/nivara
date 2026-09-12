package com.example.rental_management.property.service;

import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.entity.PropertyManager;
import com.example.rental_management.property.entity.PropertyManagerId;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.repository.PropertyRepository;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PropertyManagerService {
    private final PropertyManagerRepository propertyManagerRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    public PropertyManagerService(PropertyManagerRepository propertyManagerRepository, PropertyRepository propertyRepository, UserRepository userRepository) {
        this.propertyManagerRepository = propertyManagerRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public PropertyManager assignManager(
            Long propertyId,
            Long ownerId,
            Long managerId) {
        Property property = propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new RuntimeException("property not found"));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("manager not found"));
        if (manager.getRole() != Role.MANAGER) {
            throw new RuntimeException("user is not a manager");
        }
        PropertyManagerId id = new PropertyManagerId(propertyId, managerId);
        if (propertyManagerRepository.existsById(id)) {
            throw new RuntimeException("manager already assigned");
        }
        PropertyManager propertyManager = new PropertyManager();
        propertyManager.setId(id);
        propertyManager.setProperty(property);
        propertyManager.setManager(manager);
        propertyManager.setAssignedAt(LocalDateTime.now());
        return propertyManagerRepository.save(propertyManager);
    }
    public void removeManager(
            Long propertyId,
            Long ownerId,
            Long managerId) {

        propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new RuntimeException("property not found"));

        PropertyManagerId id =
                new PropertyManagerId(propertyId, managerId);

        if (!propertyManagerRepository.existsById(id)) {
            throw new RuntimeException("manager is not assigned to this property");
        }

        propertyManagerRepository.deleteById(id);
    }


}
