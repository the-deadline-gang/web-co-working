package com.cosync.booking.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cosync.booking.entity.enumtype.EquipmentStatus;

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
@Table(name = "equipment")
public class Equipment {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id", length = 20 , nullable=false)
    private Long id;  

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @ManyToOne(fetch = FetchType.LAZY)              
    @JoinColumn(name = "resource_id")           
    private Resource resource;

    @Column(name ="barcode" , length=30,nullable=false )
    private String barcode ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;
    
    @CreationTimestamp 
    @Column(name = "created_at" , nullable=  false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Equipment(){}

    public Equipment(EquipmentType type,String barcode,Resource resource){
        this.equipmentType = type;
        this.barcode = barcode;
        this.resource = resource;
    }

  

    // ---------- getter ----------

    public Long getId() { return id; }
    public EquipmentType getEquipmentType() { return equipmentType; }
    /** null = ของให้ยืมที่ Counter, มีค่า = อุปกรณ์ประจำห้องนั้น */
    public Resource getResource() { return resource; }
    public String getBarcode() { return barcode; }
    public EquipmentStatus getStatus() { return status; }

   
}
