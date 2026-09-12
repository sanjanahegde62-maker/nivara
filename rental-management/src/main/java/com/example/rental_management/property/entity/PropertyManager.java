package com.example.rental_management.property.entity;

import com.example.rental_management.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="property_managers")
@Getter
@Setter
@NoArgsConstructor
public class PropertyManager {
    @EmbeddedId
    private PropertyManagerId id;
    @ManyToOne(fetch=FetchType.LAZY)
    @MapsId("propertyId")
    @JoinColumn(name="property_id")

    private Property property;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("managerId")
    @JoinColumn(name="manager_id")
    private User manager;

    @Column(nullable = false)
    private LocalDateTime assignedAt;
}
