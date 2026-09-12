package com.example.rental_management.property.service;

import com.example.rental_management.property.dto.UnitRequest;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyRepository;
import com.example.rental_management.property.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitService {
    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    public UnitService(UnitRepository unitRepository, PropertyRepository propertyRepository) {
        this.unitRepository = unitRepository;
        this.propertyRepository = propertyRepository;
    }
    public Unit createUnit(
            Long propertyId,
            Long ownerId,
            UnitRequest request){
        Property property = propertyRepository.findByIdAndOwnerId(propertyId,ownerId)
                .orElseThrow(()-> new RuntimeException("property not found"));
        Unit unit=new Unit();
        unit.setProperty(property);
        unit.setUnitNumber(request.unitNumber());
        unit.setUnitType(request.unitType());
        unit.setMonthlyRent(request.monthlyRent());
        unit.setStatus(request.status());
        return unitRepository.save(unit);
    }
    public List<Unit> getUnits(
            Long propertyId,
            Long ownerId){
        propertyRepository.findByIdAndOwnerId(propertyId,ownerId)
                .orElseThrow(()-> new RuntimeException("property not found"));
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
