package com.cosync.booking.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cosync.booking.entity.enumtype.ServiceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name = "resource")
public class Resource {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long id ;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "code" ,  unique = true,nullable = false, length = 20)
    private String code;

    @Column(name = "capacity" , nullable = false )
    private int capacity;

    @Column(name = "daily_quota" )
    private Integer dailyQuota;

    @Column(name = "size_sqm"  ,precision = 6, scale = 2)
    private BigDecimal sizeSqm;

    @Column(name = "image_url" )
    private String imageUrl;

    @Column(name = "is_active")
    private boolean active;

    @CreationTimestamp 
    @Column(name = "created_at", updatable = false ,nullable = false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    protected Resource() {}


    private Resource(Zone zone, String code, ServiceType serviceType, int capacity,
            Integer dailyQuota, BigDecimal sizeSqm, String imageUrl) {
    this.zone = zone;
    this.code = code;
    this.serviceType = serviceType;
    this.capacity = capacity;
    this.dailyQuota = dailyQuota;
    this.sizeSqm = sizeSqm;
    this.imageUrl = imageUrl;
    this.active = true;
    }

    /** โซน Day Pass: ประเภทเป็น DAY_PASS เสมอ (ไม่ต้องรับมา) และต้องมีโควตา */
    public static Resource dayPass(Zone zone, String code, int capacity, int dailyQuota,BigDecimal sizeSqm, String imageUrl) {
        if (dailyQuota < 1) {
            throw new IllegalArgumentException("Day Pass ต้องมีโควตาอย่างน้อย 1");
        }
        return new Resource(zone, code, ServiceType.DAY_PASS, capacity, dailyQuota, sizeSqm, imageUrl);
    }

    /** โต๊ะหรือห้อง: ห้ามเป็น DAY_PASS และไม่มีโควตา (ไม่ต้องรับมาเลย) */
    public static Resource bookable(Zone zone, String code, ServiceType serviceType, int capacity,BigDecimal sizeSqm, String imageUrl) {
        if (serviceType == ServiceType.DAY_PASS) {
            throw new IllegalArgumentException("Day Pass ให้ใช้ Resource.dayPass()");
        }
        return new Resource(zone, code, serviceType, capacity, null, sizeSqm, imageUrl);
    }

  
    public String getCode (){
        return  this.code;
    }

    public int getCapacity(){
        return  this.capacity;
    }

    public Integer getDailyQuota(){
        return this.dailyQuota;
    }

    public BigDecimal getSizeSqm(){
        return this.sizeSqm;
    }
    public String getImageUrl(){
        return this.imageUrl;
    }

    public boolean isActive(){
        return this.active;
    }


    public Long getId(){
        return this.id;
    }
    public Zone getZone(){
        return this.zone;
    }
    public ServiceType getServiceType(){
        return this.serviceType;
    }


        
}
