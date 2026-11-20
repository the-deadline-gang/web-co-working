package com.cosync.booking.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity 
@Table(name = "booking_participant")
public class BookingParticipant {
    
    @EmbeddedId
    private BookingParticipantId id;

    @MapsId("bookingId")                          //  ค่า booking_id ใน id มาจาก booking ตัวนี้
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    
    @Column(name = "invited_at" , nullable= false)
    private Instant invitedAt;

    @Column(name = "left_at" )
    private Instant leftAt;
    
    @CreationTimestamp 
    @Column(name = "created_at" , nullable=  false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookingParticipant(){}

    public BookingParticipant(Booking booking, Long userId, Instant now){
      this.id = new BookingParticipantId(booking.getId(),userId);
      this.booking = booking;                                         
      this.invitedAt = now;  
      
    }

    /**
     * ไว้บันทึกเวลา โดยจะมีการเช็คว่าเคยออกไปแล้วหรือยัง
     * @param now เวลาที่ออก
     */
    public void leave(Instant now){
        if (this.leftAt != null) {
            throw new IllegalStateException("ออกไปแล้ว");
        }
        this.leftAt = now;
    }

    /**
     * การเชิญกลับเข้ามาใหม่ จะทำการบันทึกเวลาออกเป็น null และก็จะบันทึกเวลาที่เชิญเป็นปัจจุบัน
     * โดยจะมีการเช็คว่าคนนั้นใข้งานอยู่ไหม
     * @param now เวลาที่เข้า
     */
    public void rejoin(Instant now){
        if (this.leftAt == null) {
            throw new IllegalStateException("ยังเป็นผู้ร่วมใช้อยู่ เชิญซ้ำไม่ได้");
        }
        this.leftAt = null;
        this.invitedAt = now;
    }

    // ---------- getter ----------

    public BookingParticipantId getId() { return id; }
    public Long getUserId() { return id.getUserId(); }
    public Booking getBooking() { return booking; }
    public Instant getInvitedAt() { return invitedAt; }
    /** null = ยังอยู่ในการจอง */
    public Instant getLeftAt() { return leftAt; }

    /** ยังเป็นผู้ร่วมใช้อยู่ (ยังไม่ออกหรือไม่ถูกลบ) */
    public boolean isActive() { return leftAt == null; }
}
