package com.example.rental_management.property.service;

import com.example.rental_management.property.dto.PropertyRequest;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import java.util.List;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public PropertyService(PropertyRepository propertyRepository, PropertyManagerRepository propertyManagerRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }
    public List<Property> getMyProperties(Long ownerId){
        return propertyRepository.findByOwnerId(ownerId);
    }
    public Property getMyProperty(Long propertyId,Long ownerId) {
        return propertyRepository
                .findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new RuntimeException("property not found"));
    }
    public Property createProperty(Property property){
        return propertyRepository.save(property);
    }
 public Property updateProperty(Long propertyId, Long ownerId, PropertyRequest request){
        Property property=propertyRepository.findByIdAndOwnerId(propertyId,ownerId)
                .orElseThrow(()-> new RuntimeException("property not found"));
        property.setName(request.name());
        property.setAddress(request.address());
        property.setPropertyType(request.propertyType());
        property.setDescription(request.description());
        return propertyRepository.save(property);
 }
 public void deleteProperty(Long propertyId,Long ownerId){
        Property property=propertyRepository.findByIdAndOwnerId(propertyId,ownerId)
                .orElseThrow(()-> new RuntimeException(("property not found")));
        propertyRepository.delete(property);
 }
 public Property getPropertyForManager(
         Long propertyId,
         Long managerId){
        return propertyManagerRepository.findByPropertyIdAndManagerId(propertyId,managerId)
                .orElseThrow(()->new RuntimeException("property not found"))
                .getProperty();
 }


}

