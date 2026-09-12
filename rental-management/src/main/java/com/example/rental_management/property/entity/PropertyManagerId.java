package com.example.rental_management.property.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PropertyManagerId implements Serializable {
    private Long propertyId;
    private Long managerId;}


