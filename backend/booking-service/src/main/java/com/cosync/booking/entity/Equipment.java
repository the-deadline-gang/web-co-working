package com.cosync.booking.entity;

import java.time.Instant;
import java.util.Arrays;

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
    @Column(name = "equipment_id" , nullable=false)
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

    /** 
     * เอาไว้ update ข้อมูลของ Equipment
     * @param type ประเภทของ Equipment
     * @param barcode barcode ของ Equipment
     * @param resource  Resource
     */
    public void update(EquipmentType type, String barcode, Resource resource){
        requireStatus("update", EquipmentStatus.AVAILABLE, EquipmentStatus.MAINTENANCE);
        this.equipmentType = type;
        this.resource = resource;
        this.barcode = barcode;

    }


    /**
     * เอาไว้เปลี่ยน status ว่าอุปกรณ์นี้ถูกยืม
     * โดยจะมีการเช็คก่อนว่า status ก่อนหน้าเป็น AVAILABLE ไหม
     */
    public void markBorrowed(){
        requireStatus("markBorrowed", EquipmentStatus.AVAILABLE);
        this.status = EquipmentStatus.BORROWED;
    }

    /**
     * บันทึกการคืนของซึ่งจะบันทึกว่าต้องซ่อมหรือไม่
     * โดยจะมีการเช็คว่า status เป็น  BORROWED ก่อนไหม
     * @param needsRepair ต้องการซ่อมไหม
     */ 
    public void markReturned(boolean needsRepair){
        requireStatus("markReturned", EquipmentStatus.BORROWED);
        if (needsRepair) {
            this.status = EquipmentStatus.MAINTENANCE;
        }else{
            this.status = EquipmentStatus.AVAILABLE;
        }

    }

    /**
     * เอาไว้เปลี่ยน status ระหว่าง  AVAILABLE กับ MAINTENANCE
     * โดยจะมีการเช็คว่าให้ใส่มาได้แค่ AVAILABLE กับ MAINTENANCE
     * @param to จะเปลี่ยน status เป็นอะไร AVAILABLE กับ MAINTENANCE
     */
    public void changeStatus(EquipmentStatus to) {
        requireStatus("changeStatus", EquipmentStatus.AVAILABLE, EquipmentStatus.MAINTENANCE);
        if (to != EquipmentStatus.AVAILABLE && to != EquipmentStatus.MAINTENANCE) {
            throw new IllegalArgumentException("เปลี่ยนได้เฉพาะ AVAILABLE หรือ MAINTENANCE แต่ได้ " + to);
        }
        this.status = to;
    }

    /**
     * เอาไว้บันทึกว่าของหาย
     */
    public void markLost(){
        requireStatus("markLost", EquipmentStatus.BORROWED);
        this.status = EquipmentStatus.LOST;
    }
    

    /**
     * เอาไว้เช็ค status ว่าเป็นแบบที่ต้องการไหม
     * @param action กำลังทำอะไร
     * @param allowed status อะไรบ้าง
     */
    private void requireStatus(String action, EquipmentStatus... allowed) {
        for (EquipmentStatus s : allowed) {
            if (status == s) return;          // ตรงกับตัวใดตัวหนึ่ง = ผ่าน
        }
         throw new IllegalStateException(action + " ได้เฉพาะตอน " + Arrays.toString(allowed)  + " แต่ตอนนี้เป็น " + status);
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public EquipmentType getEquipmentType() { return equipmentType; }
    /** null = ของให้ยืมที่ Counter, มีค่า = อุปกรณ์ประจำห้องนั้น */
    public Resource getResource() { return resource; }
    public String getBarcode() { return barcode; }
    public EquipmentStatus getStatus() { return status; }

   
}
