package com.cosync.booking.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import com.cosync.booking.entity.enumtype.AccessDirection;
import com.cosync.booking.entity.enumtype.AccessMethod;
import com.cosync.booking.entity.enumtype.OverrideReason;

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

@Immutable
@Entity
@Table(name = "access_log")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id" , nullable=false)
    private Long id;

    @Column(name = "user_id" , nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)              
    @JoinColumn(name = "booking_id")  
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction" , nullable = false , length=3)
    private AccessDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private AccessMethod method;
        
    @Column(name = "override_by" , length=20)
    private String overrideBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "override_reason" , length=30)
    private OverrideReason overrideReason;

    @Column(name = "override_note" , length=200)
    private String overrideNote;

    @Column(name = "scanned_at" ,nullable=false)
    private Instant scannedAt;

    @CreationTimestamp 
    @Column(name = "created_at" , nullable=  false)
    private Instant createdAt;

    protected  AccessLog(){}

    private AccessLog(Long userId, Booking booking, AccessDirection direction, AccessMethod method,
        String overrideBy, OverrideReason overrideReason, String overrideNote, Instant scannedAt) {
        this.userId = userId;
        this.booking = booking;
        this.direction = direction;
        this.method = method;
        this.overrideBy = overrideBy;
        this.overrideReason = overrideReason;
        this.overrideNote = overrideNote;
        this.scannedAt = scannedAt;
    }

    /**
     * ใช้สำหรับสแกนเข้าแบบ Qr-code โดยจะมีการเช็คว่ามีการจองจริงรึป่าว
     * @param userId id ของผู้ใช้
     * @param booking การจอง
     * @param now เวลาที่เข้า scan
     * @return เป็นการบันทึก log 
     */
    public static AccessLog scanIn(Long userId, Booking booking, Instant now) {
        if (booking == null) {
            throw new IllegalArgumentException("การเข้าต้องมีการจองเสมอ");
        }
        return new AccessLog(userId, booking, AccessDirection.IN, AccessMethod.QR, null, null, null, now);
    }

   /**
     * ใช้สำหรับสแกนออกแบบ Qr-code 
     * @param userId id ของผู้ใช้
     * @param booking การจอง
     * @param now เวลาที่ออก
     * @return เป็นการบันทึก log 
     */
    public static AccessLog scanOut(Long userId, Booking booking, Instant now){

        return new AccessLog(userId, booking, AccessDirection.OUT, AccessMethod.QR, null, null, null, now);
    }


    /**
     * บันทึกการออกโดยที่ต้องมีคนอนุญาตในกรณีที่ไม่ได้ scan ออกตามปกติ
     * @param userId id ของผู้ใช้
     * @param booking การจอง
     * @param now เวลาที่ออก
     * @param overrideBy ใครเป็นคนอนุญาต
     * @param overrideReason เหตุผล
     * @param overrideNote Note รายละเอียดเกี่ยวกับการออกครั้งนี้
     * @return เป็นการบันทึก log 
     */
    public static AccessLog override(Long userId, Booking booking, Instant now,String overrideBy,OverrideReason overrideReason,String overrideNote){
        if(overrideBy == null || overrideBy.isBlank() ){
            throw new IllegalArgumentException("ต้องมีการลงชื่อว่าใครอนุญาต");
        }
        if(overrideReason == null ){
            throw new IllegalArgumentException("ต้องมีเหตุผลในการอนุญาต");
        }

        return new AccessLog(userId, booking, AccessDirection.OUT, AccessMethod.OVERRIDE, overrideBy, overrideReason, overrideNote, now);
    }

    /**
     * เป็นการออกแบบ auto โดยจะทำการบันทึกการออก
     * @param userId id ของผู้ใช้
     * @param booking การจอง
     * @param now เวลาที่ออก
     * @return เป็นการบันทึก log 
     */
    public static AccessLog autoCheckout(Long userId, Booking booking, Instant now){
        return new AccessLog(userId, booking, AccessDirection.OUT, AccessMethod.AUTO, null, null, null, now);
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    /** การจองที่ใช้เข้า ตอนออกอาจเป็น null */
    public Booking getBooking() { return booking; }
    public AccessDirection getDirection() { return direction; }
    public AccessMethod getMethod() { return method; }
    /** มีค่าเฉพาะ OVERRIDE เช่น STF-005 */
    public String getOverrideBy() { return overrideBy; }
    public OverrideReason getOverrideReason() { return overrideReason; }
    public String getOverrideNote() { return overrideNote; }
    public Instant getScannedAt() { return scannedAt; }
}
