package com.cosync.booking.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cosync.booking.entity.enumtype.BookingStatus;
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
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id" , nullable=false)
    private Long id; 
    

    @Column(name = "user_id", nullable=false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(name = "start_at" , nullable=false)
    private Instant startAt;

    @Column(name = "end_at" , nullable=false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false)
    private BookingStatus  status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "price",nullable=false , precision = 12, scale = 2)
    private BigDecimal price ;

    @Column(name = "hold_until")
    private Instant holdUntil;

    @Column(name = "hold_extended" , nullable=false)
    private boolean holdExtended = false;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount ;

    @CreationTimestamp 
    @Column(name = "created_at" , nullable=  false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking(){}

    public static Booking hold(Long userId, Resource resource, Instant startAt, Instant endAt,
        BigDecimal price, Instant now, int holdMinutes){
            if (!endAt.isAfter(startAt)) {
                throw new IllegalArgumentException("endAt ต้องอยู่หลัง startAt");
            }

            if(price.signum() < 0){
                throw new IllegalArgumentException("price ต้องไม่ติดลบ");
            }
            Booking booking = new Booking();                
            booking.userId = userId;
            booking.resource = resource;
            booking.serviceType = resource.getServiceType(); // คัดลอกจาก resource
            booking.startAt = startAt;
            booking.endAt = endAt;
            booking.price = price;
            booking.holdUntil = now.plus(holdMinutes, ChronoUnit.MINUTES);
            return booking;
    }

    /** ชำระเงินสำเร็จ (F4 ขั้น 5) เงินถูกตัดตอนเข้าสถานะนี้เท่านั้น */
    /**
     * เป็นการ  confirm การจองโดยที่จะมีการเช็ค status ก่อนว่าเป็น PENDING_PAYMENT ก่อนไหมถึงค่อนบันทึกค่า
     * @param now
     */
    public void confirm(Instant now) {
        requireStatus(BookingStatus.PENDING_PAYMENT, "confirm");
        this.status = BookingStatus.CONFIRMED;
        this.paidAt = now;
        this.holdUntil = null;          // ไม่มีการล็อกแล้ว จ่ายเสร็จแล้ว
    }

    /**
     * เปลี่ยน status เป็น EXPIRED
     */
    public void expire(){   
        requireStatus(BookingStatus.PENDING_PAYMENT, "expire");
        this.status = BookingStatus.EXPIRED;
        this.holdUntil = null;
    }

    /**
     * เอาไว้ใช้ต่อเวลา ซึ่งจะมีการเช็คก่่อนว่าเคยต่อเวลาไปแล้วหรือยัง
     * @param minutes นาทีที่จะต่อเวลา
     */
    public void extendHold(int minutes){
        requireStatus(BookingStatus.PENDING_PAYMENT, "extendHold");
        if ( this.holdExtended) {
            throw new IllegalStateException( "ต่อเวลาได้ครั้งเดียว และต่อไปแล้ว");
        }
        this.holdUntil = this.holdUntil.plus(minutes, ChronoUnit.MINUTES);
        this.holdExtended = true;
    }

    /**
     * เอาไว้ยกเลิกการจองโดนจะมีการเช็คก่อนว่า
     * status เป็น Confirmed ก่อนไหม ต่อมาจะเป็นการเช็คว่า refund มีค่าเป็นบวกไหม
     * และจะเช็คว่า refund มีค่ามากกว่า price ไหม 
     * และก็ทำการบันทึกค่า  refundAmount , cancelledAt , status
     * @param now เวลาปัจจุบันในการที่จะยกเลิก
     * @param refund ต้องคืนเงินเท่าไหร่
     */
    public void cancel(Instant now, BigDecimal refund){
        requireStatus(BookingStatus.CONFIRMED, "cancel");
        if (refund.signum() < 0 ) {
            throw new IllegalArgumentException( "refund ต้องมีค่าเป็นบวก");
        }
        if(refund.compareTo(this.price) > 0){
            throw new IllegalArgumentException( "refund ต้องมีค่าไม่เกิน price");
        }
        this.cancelledAt = now;
        this.refundAmount = refund;
        this.status = BookingStatus.CANCELLED;
    }

    /**
     * บันทึกเวลาในการ CheckIn โดยจะเช็คก่อนว่าการจองนี้มี status เป็น Confirmed แล้ว
     * @param now
     */
    public void checkIn(Instant now){
        requireStatus(BookingStatus.CONFIRMED, "checkIn");
        this.checkedInAt = now;
        this.status = BookingStatus.CHECKED_IN;
    }
    
    /** 
     * เป็นการบันทึก booking status เป็น markNoShow โดยจะเช็คก่อนว่า status เดิมเป็น CONFIRMED ไหม
     */
    public void markNoShow(){
        requireStatus(BookingStatus.CONFIRMED, "markNoShow");
        this.status = BookingStatus.NO_SHOW;
    }

    /** 
     * เป็นการบันทึก booking status เป็น complete โดยจะเช็คก่อนว่า status เดิมเป็น CHECKED_IN ไหม
     */
    public void complete(){
        requireStatus(BookingStatus.CHECKED_IN, "complete");
        this.status = BookingStatus.COMPLETED;
    }
 
    /**
     * เอาไว้เช็คว่า status ของ booking นี้มันตรงกับที่เราต้องการไหม
     * @param expected status ที่จะเช็ค
     * @param action เป็นตอนใช้กับอะไร
     */
    private void requireStatus(BookingStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(action + " ได้เฉพาะตอน " + expected + " แต่ตอนนี้เป็น " + status);
        }
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    /** เจ้าของการจอง (user อยู่ใน identity_db จึงเก็บแค่ id) */
    public Long getUserId() { return userId; }
    public Resource getResource() { return resource; }
    public ServiceType getServiceType() { return serviceType; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getPrice() { return price; }

    /** มีค่าเฉพาะตอน PENDING_PAYMENT */
    public Instant getHoldUntil() { return holdUntil; }
    public boolean isHoldExtended() { return holdExtended; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public Instant getCancelledAt() { return cancelledAt; }


    /** มีค่าเฉพาะตอน CANCELLED (อาจเป็น 0) */
    public BigDecimal getRefundAmount() { return refundAmount; }
    public Instant getCreatedAt() { return createdAt; }
}

