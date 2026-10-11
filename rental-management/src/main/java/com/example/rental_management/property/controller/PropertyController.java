package com.example.rental_management.property.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.property.dto.*;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.service.PropertyManagerService;
import com.example.rental_management.property.service.PropertyService;
import com.example.rental_management.property.service.UnitService;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyManagerService propertyManagerService;
    private final PropertyManagerRepository propertyManagerRepository;
    private final UnitService unitService;

    public PropertyController(PropertyService propertyService,
                              PropertyManagerService propertyManagerService,
                              PropertyManagerRepository propertyManagerRepository,
                              UnitService unitService) {
        this.propertyService = propertyService;
        this.propertyManagerService = propertyManagerService;
        this.propertyManagerRepository = propertyManagerRepository;
        this.unitService = unitService;
    }

    /** GET /properties — list all properties visible to the current user */
    @GetMapping
    public List<PropertyResponse> listProperties() {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        if (role == Role.OWNER) {
            return propertyService.getMyProperties(userId)
                    .stream().map(this::toResponse).toList();
        } else if (role == Role.MANAGER) {
            return propertyManagerRepository.findByManagerId(userId)
                    .stream().map(pm -> toResponse(pm.getProperty())).toList();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only owners and managers can list properties");
        }
    }

    /**
     * GET /properties/{id} — full property detail with units and managers.
     * OWNER sees own properties; MANAGER sees assigned properties.
     */
    @GetMapping("/{id}")
    public PropertyDetailResponse getProperty(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();

        Property property;
        if (role == Role.OWNER) {
            property = propertyService.getMyProperty(id, userId);
        } else if (role == Role.MANAGER) {
            property = propertyService.getPropertyForManager(id, userId);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not allowed to access this property");
        }

        // Units — authorization already verified above
        var units = unitService.getUnitsForProperty(id)
                .stream()
                .map(u -> new UnitResponse(
                        u.getId(),
                        u.getProperty().getId(),
                        u.getUnitNumber(),
                        u.getUnitType(),
                        u.getMonthlyRent(),
                        u.getStatus(),
                        u.getCreatedAt().toString(),
                        u.getUpdatedAt().toString()))
                .toList();

        // Managers
        var managers = propertyManagerRepository.findByPropertyId(id)
                .stream()
                .map(ManagerAssignmentResponse::from)
                .toList();

        int occupied = (int) units.stream()
                .filter(u -> "OCCUPIED".equalsIgnoreCase(u.status()))
                .count();

        return new PropertyDetailResponse(
                property.getId(),
                property.getName(),
                property.getAddress(),
                property.getPropertyType(),
                property.getDescription(),
                units.size(),
                occupied,
                units.size() - occupied,
                property.getCreatedAt(),
                property.getUpdatedAt(),
                units,
                managers
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse createProperty(@RequestBody PropertyRequest request) {
        requireOwner();
        Long ownerId = SecurityUtil.getCurrentUserId();
        Property property = new Property();
        property.setName(request.name());
        property.setAddress(request.address());
        property.setPropertyType(request.propertyType());
        property.setDescription(request.description());
        User owner = new User();
        owner.setId(ownerId);
        property.setOwner(owner);
        Property createdProperty = propertyService.createProperty(property);
        return toResponse(createdProperty);
    }

    @PutMapping("/{id}")
    public PropertyResponse updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyRequest request) {
        requireOwner();
        Long ownerId = SecurityUtil.getCurrentUserId();
        Property updatedProperty = propertyService.updateProperty(id, ownerId, request);
        return toResponse(updatedProperty);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProperty(@PathVariable Long id) {
        requireOwner();
        Long ownerId = SecurityUtil.getCurrentUserId();
        propertyService.deleteProperty(id, ownerId);
    }

    /** GET /properties/{propertyId}/managers — list managers assigned to a property */
    @GetMapping("/{propertyId}/managers")
    public List<ManagerAssignmentResponse> listManagers(@PathVariable Long propertyId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Role role = SecurityUtil.getCurrentUserRole();
        // Verify the caller has access to this property
        if (role == Role.OWNER) {
            propertyService.getMyProperty(propertyId, userId);
        } else if (role == Role.MANAGER) {
            propertyService.getPropertyForManager(propertyId, userId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return propertyManagerRepository.findByPropertyId(propertyId)
                .stream()
                .map(ManagerAssignmentResponse::from)
                .toList();
    }

    @PostMapping("/{propertyId}/managers/{managerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignManager(@PathVariable Long propertyId, @PathVariable Long managerId) {
        requireOwner();
        Long ownerId = SecurityUtil.getCurrentUserId();
        propertyManagerService.assignManager(propertyId, ownerId, managerId);
    }

    @DeleteMapping("/{propertyId}/managers/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeManager(
            @PathVariable Long propertyId,
            @PathVariable Long managerId) {
        requireOwner();
        Long ownerId = SecurityUtil.getCurrentUserId();
        propertyManagerService.removeManager(propertyId, ownerId, managerId);
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getAddress(),
                property.getPropertyType(),
                property.getDescription(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }

    private void requireOwner() {
        if (SecurityUtil.getCurrentUserRole() != Role.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only property owners can perform this action");
        }
    }
}
