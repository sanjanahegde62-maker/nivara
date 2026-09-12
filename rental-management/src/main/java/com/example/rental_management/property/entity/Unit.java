package com.example.rental_management.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "units")
@Getter
@Setter
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="property_id",nullable=false)
    private Property property;

    @Column(nullable = false)
    private String unitNumber;
    @Column(nullable = false)
    private String unitType;
    @Column(nullable = false)
    private BigDecimal monthlyRent;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
        updatedAt=LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now();
    }

}
