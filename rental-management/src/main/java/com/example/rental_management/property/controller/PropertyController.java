package com.example.rental_management.property.controller;
import com.example.rental_management.property.dto.PropertyRequest;
import com.example.rental_management.property.dto.PropertyResponse;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.service.PropertyManagerService;
import com.example.rental_management.property.service.PropertyService;
import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")

public class PropertyController {
    private final PropertyService propertyService;
    private final PropertyManagerService propertyManagerService;
    public PropertyController(PropertyService propertyService, PropertyManagerService propertyManagerService) {
        this.propertyService = propertyService;
        this.propertyManagerService = propertyManagerService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse createProperty(@RequestBody PropertyRequest request){
      Long ownerId= SecurityUtil.getCurrentUserId();
      Property property=new Property();
        property.setName(request.name());
        property.setAddress(request.address());
        property.setPropertyType(request.propertyType());
        property.setDescription(request.description());
        User owner=new User();
        owner.setId(ownerId);
        property.setOwner(owner);
        Property createdProperty=propertyService.createProperty(property);
        return toResponse(createdProperty);

    }
    @GetMapping("/{id}")

    public PropertyResponse getProperty(@PathVariable Long id) {

        Long userId = SecurityUtil.getCurrentUserId();

        Property property;

        if (SecurityUtil.getCurrentUserRole().equals("ROLE_OWNER")) {
            property = propertyService.getMyProperty(id, userId);
        } else {
            property = propertyService.getPropertyForManager(id, userId);
        }

        return toResponse(property);
    }
    private PropertyResponse toResponse(Property property){
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
    @PutMapping("/{id}")
    public PropertyResponse updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyRequest request) {
        Long ownerId=SecurityUtil.getCurrentUserId();
        Property updatedProperty=propertyService.updateProperty(id,ownerId,request);
        return toResponse(updatedProperty);

    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProperty(@PathVariable Long id){
        Long ownerId=SecurityUtil.getCurrentUserId();
        propertyService.deleteProperty(id,ownerId);
    }
    @PostMapping("/{propertyId}/managers/{managerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignManager(@PathVariable Long propertyId,@PathVariable Long managerId){
        Long ownerId = SecurityUtil.getCurrentUserId();
        propertyManagerService.assignManager(
                propertyId,
                ownerId,
                managerId
        );
    }
    @DeleteMapping("/{propertyId}/managers/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeManager(
            @PathVariable Long propertyId,
            @PathVariable Long managerId) {

        Long ownerId = SecurityUtil.getCurrentUserId();

        propertyManagerService.removeManager(
                propertyId,
                ownerId,
                managerId
        );
    }

}
