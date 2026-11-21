package com.cosync.booking.entity;

import java.math.BigDecimal;
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
@Table(name = "equipment_type")
public class EquipmentType {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_type_id" , nullable= false)
    private Long equipmentTypeId;  

    @Column(name = "name", length = 100, nullable= false)
    private String name;


    @Column(name = "is_loanable",nullable= false)
    private boolean isLoanable;

    @Column(name = "loan_fee" ,precision = 12, scale = 2)
    private BigDecimal loanFee;

    @Column(name = "deposit" ,precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "pricing_updated_by", length = 20)
    private String pricingUpdatedBy;

    @CreationTimestamp 
    @Column(name = "created_at" , nullable=  false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected EquipmentType(){}
    

    public EquipmentType(String name, boolean loanable){
        this.name = name ;
        this.isLoanable = loanable;
    }

    /**
     * ตั้งค่า fee กับ deposit  และมีการบันทึกว่าใครเป็นคนตั้งค่าใหม่
     * โดยจะมีการเช็คก่อนว่า fee กับ deposit ติดลบไหม
     * @param fee
     * @param deposit
     * @param by
     */
    public void setPricing(BigDecimal fee,BigDecimal deposit , String by){

        if (fee.signum() < 0) {
            throw new IllegalArgumentException("fee ต้องไม่ติดลบ ");
        }
        if (deposit.signum() < 0) {
            throw new IllegalArgumentException("deposit ต้องไม่ติดลบ ");
        }
        this.loanFee = fee;
        this.deposit = deposit;
        this.pricingUpdatedBy = by;
    }

    // ---------- getter ----------

    public Long getId() { return equipmentTypeId; }
    public String getName() { return name; }
    public boolean isLoanable() { return isLoanable; }
    public BigDecimal getLoanFee() { return loanFee; }
    public BigDecimal getDeposit() { return deposit; }
    public String getPricingUpdatedBy() { return pricingUpdatedBy; }

}

