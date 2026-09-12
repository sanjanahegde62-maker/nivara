package com.example.rental_management.property.entity;

import com.example.rental_management.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@Getter
@Setter
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String propertyType;
    private String description;
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
