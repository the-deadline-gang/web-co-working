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
    /**
     * ใช้สำหรับการจอง Daypass อย่างเดียว
     * โดยจะมีการเช็คว่าใส่ค่า dailyQuota มามากกว่า 1 รึป่าว
     * @param zone Zone ที่จะจอง
     * @param code รหัส code
     * @param capacity ความจุของคน
     * @param dailyQuota จำนวนต่อวัน
     * @param sizeSqm ขนาดตารางของ
     * @param imageUrl รูปภาพของห้อง
     * @return
     */
    public static Resource dayPass(Zone zone, String code, int capacity, int dailyQuota,BigDecimal sizeSqm, String imageUrl) {
        requireQuotaMatchesType(ServiceType.DAY_PASS, dailyQuota);
    
        return new Resource(zone, code, ServiceType.DAY_PASS, capacity, dailyQuota, sizeSqm, imageUrl);
    }

   
    /**
     * การจองทุกแบบยกเว้นแบบ Daypass 
     * โดยจะมีการเช็คด่้วยว่าเป็น Daypass รึป่าว 
     * @param zone Zone ที่จะจอง
     * @param code รหัส code
     * @param serviceType ประเภทของ service 
     * @param capacity ความจุของคน
     * @param sizeSqm ขนาดตารางของ
     * @param imageUrl รูปภาพของห้อง
     * @return
     */
    public static Resource bookable(Zone zone, String code, ServiceType serviceType, int capacity,BigDecimal sizeSqm, String imageUrl) {
        if (serviceType == ServiceType.DAY_PASS) {
            throw new IllegalArgumentException("Day Pass ให้ใช้ Resource.dayPass()");
        }
        requireQuotaMatchesType(serviceType, null);
      
      
        return new Resource(zone, code, serviceType, capacity, null, sizeSqm, imageUrl);
    }

    /**
     * เอาไว้เช็คว่าส่งข้อมูล type กับ dailyQuota ถูกไหม
     * โดยที่ถ้า service เป็น daypass ควรจะมี dailyQuota แต่ถ้าเป็น service จะไม่ควรมี
     * @param type
     * @param dailyQuota
     */
    private static void requireQuotaMatchesType(ServiceType type, Integer dailyQuota) { 
        if (type == ServiceType.DAY_PASS) {
            if ( dailyQuota == null || dailyQuota < 1 ) {
                throw new IllegalArgumentException("Day Pass ต้องมีโควตาอย่างน้อย 1");
            }
         
        }else{
            if (dailyQuota != null) {
                throw new IllegalArgumentException("Service ตัวอื่นไม่ต้องใส่ค่า dailyQuota ");
            }
        }

    }

    public void update(Zone zone, String code, ServiceType serviceType, int capacity,Integer dailyQuota, BigDecimal sizeSqm, String imageUrl,boolean  active){
        requireQuotaMatchesType(serviceType, dailyQuota);
        this.zone = zone;
        this.code = code;
        this.serviceType = serviceType;
        this.capacity = capacity;
        this.dailyQuota = dailyQuota ;
        this.sizeSqm = sizeSqm;
        this.imageUrl = imageUrl ;
        this.active = active;
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
