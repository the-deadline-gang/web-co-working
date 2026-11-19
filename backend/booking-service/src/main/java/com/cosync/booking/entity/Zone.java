package com.cosync.booking.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name ="zone")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @CreationTimestamp 
    @Column(name = "created_at", updatable = false,nullable = false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected Zone(){}

    public Zone(String name) {
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public Long getId(){
        return this.id;
    }

  



}