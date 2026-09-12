package com.example.rental_management.property.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.property.dto.UnitRequest;
import com.example.rental_management.property.dto.UnitResponse;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.service.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties/{propertyId}/units")
public class UnitController {
    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse createUnit(
            @PathVariable Long propertyId,
            @RequestBody UnitRequest request){
        Long ownerId= SecurityUtil.getCurrentUserId();
        Unit unit=unitService.createUnit(
                propertyId,
                ownerId,
                request
        );
        return toResponse(unit);
    }
    @GetMapping
    public List<UnitResponse> getUnits(
            @PathVariable Long propertyId) {

        Long ownerId = SecurityUtil.getCurrentUserId();

        return unitService
                .getUnits(propertyId, ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    private UnitResponse toResponse(Unit unit){
        return new UnitResponse(
                unit.getId(),
                unit.getProperty().getId(),
                unit.getUnitNumber(),
                unit.getUnitType(),
                unit.getMonthlyRent(),
                unit.getStatus(),
                unit.getCreatedAt().toString(),
                unit.getUpdatedAt().toString()
        );
    }
    @PutMapping("/{unitId}")
    public UnitResponse updateUnit(
            @PathVariable Long propertyId,@PathVariable Long unitId,@RequestBody UnitRequest request){
        Long ownerId=SecurityUtil.getCurrentUserId();
        Unit unit=unitService.updateUnit(unitId,ownerId,request);
        return toResponse(unit);
    }
    @DeleteMapping("/{unitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnit(@PathVariable Long unitId) {

        Long ownerId = SecurityUtil.getCurrentUserId();

        unitService.deleteUnit(unitId, ownerId);
    }


}
