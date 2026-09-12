package com.example.rental_management.property.dto;

public record PropertyRequest(
        String name,
        String address,
        String propertyType,
        String description
) {

}
