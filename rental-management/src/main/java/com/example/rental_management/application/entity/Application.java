package com.example.rental_management.application.entity;

import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor

public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "unit_id",nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "tenant_id",nullable = false)
    private User tenant;


    @Column( nullable=false)
    private String status;
    @Column(nullable = false)
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    @PrePersist
    protected void onCreate(){
        appliedAt = LocalDateTime.now();
        if(status==null){
            status="PENDING";
        }
    }

}
