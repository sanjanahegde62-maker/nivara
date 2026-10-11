package com.example.rental_management.property.service;

import com.example.rental_management.property.dto.UnitRequest;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.repository.PropertyRepository;
import com.example.rental_management.property.repository.UnitRepository;
import com.example.rental_management.user.entity.Role;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitService {
    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public UnitService(UnitRepository unitRepository,
                       PropertyRepository propertyRepository,
                       PropertyManagerRepository propertyManagerRepository) {
        this.unitRepository = unitRepository;
        this.propertyRepository = propertyRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    public Unit createUnit(
            Long propertyId,
            Long ownerId,
            UnitRequest request) {
        Property property = propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new RuntimeException("property not found"));
        Unit unit = new Unit();
        unit.setProperty(property);
        unit.setUnitNumber(request.unitNumber());
        unit.setUnitType(request.unitType());
        unit.setMonthlyRent(request.monthlyRent());
        unit.setStatus(request.status());
        return unitRepository.save(unit);
    }

    /**
     * Returns units for a property, with role-aware authorization.
     * OWNER must own the property; MANAGER must be assigned to it.
     */
    public List<Unit> getUnits(Long propertyId, Long userId, Role role) {
        if (role == Role.OWNER) {
            propertyRepository.findByIdAndOwnerId(propertyId, userId)
                    .orElseThrow(() -> new RuntimeException("property not found"));
        } else if (role == Role.MANAGER) {
            propertyManagerRepository.findByPropertyIdAndManagerId(propertyId, userId)
                    .orElseThrow(() -> new RuntimeException("property not found"));
        } else {
            throw new RuntimeException("You are not authorized to view units for this property");
        }
        return unitRepository.findByPropertyId(propertyId);
    }

    /**
     * Returns all units for a property without an ownership check.
     * Must only be called from contexts where authorization has already been verified.
     */
    public List<Unit> getUnitsForProperty(Long propertyId) {
        return unitRepository.findByPropertyId(propertyId);
    }
    public Unit updateUnit(
            Long unitId,
            Long ownerId,
            UnitRequest request){
        Unit unit=unitRepository.findById(unitId)
                .orElseThrow(()->new RuntimeException("unit not found"));
        propertyRepository.findByIdAndOwnerId(unit.getProperty().getId(),ownerId).orElseThrow(()-> new RuntimeException("property not found"));
        unit.setUnitNumber(request.unitNumber());
        unit.setUnitType(request.unitType());
        unit.setMonthlyRent(request.monthlyRent());
        unit.setStatus(request.status());
        return unitRepository.save(unit);

    }
    public void deleteUnit(Long unitId, Long ownerId) {


        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("unit not found"));

        propertyRepository.findByIdAndOwnerId(
                unit.getProperty().getId(),
                ownerId
        ).orElseThrow(() -> new RuntimeException("property not found"));

        unitRepository.delete(unit);
    }


}
