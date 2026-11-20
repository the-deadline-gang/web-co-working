package com.cosync.booking.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import com.cosync.booking.entity.enumtype.RateUnit;
import com.cosync.booking.entity.enumtype.ServiceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name ="service_rate")
public class ServiceRate {
    
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 20)
    private ServiceType serviceType;          

    @Column(name = "name" , nullable= false ,length = 50)
    private String name;

    @Column(name = "rate", nullable= false , precision = 12, scale = 2)
    private BigDecimal rate;


    @Enumerated(EnumType.STRING)
    @Column(name = "unit" , nullable = false ,length = 10) 
    private RateUnit unit;
    
    @Column(name = "discount_pct" , nullable = false , precision = 5, scale = 2)
    private BigDecimal discountPct;
    
    @Column(name = "updated_by")
    private String updatedBy;

    @UpdateTimestamp 
    @Column(name = "updated_at",  nullable = false)
    private Instant updatedAt;

    protected ServiceRate(){}

    public ServiceRate(ServiceType serviceType,String name, BigDecimal rate,RateUnit unit,BigDecimal discountPct){
        this.serviceType = serviceType;
        this.name = name;
        this.rate = rate;
        this.unit = unit;
        this.discountPct = discountPct;
    }
    

    public String getName(){
        return this.name;
    }

    public BigDecimal getRate(){
        return this.rate ;
    }

    public RateUnit getUnit(){
        return  this.unit;
    }

    public BigDecimal getDiscountPct(){
        return this.discountPct;
    }

    public ServiceType getServiceType(){
        return  this.serviceType;
    }
    public  String getUpdateBy(){
        return this.updatedBy;
    }

    public Instant getUpdateAt(){
        return this.updatedAt; 
    }


    public void update(BigDecimal rate, BigDecimal discountPct, String updatedBy){
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate ต้องมากกว่า 0");
        }
        if (discountPct.signum() < 0 || discountPct.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("discountPct ต้องอยู่ระหว่าง 0–100");
        }
        this.rate = rate;
        this.discountPct = discountPct;
        this.updatedBy = updatedBy;
    }
}
