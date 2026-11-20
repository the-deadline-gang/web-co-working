package com.cosync.booking.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BookingParticipantId implements Serializable {
    
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "user_id")
    private Long userId;

    protected BookingParticipantId() {}

    public BookingParticipantId(Long bookingId, Long userId) { 
        this.bookingId = bookingId;
        this.userId = userId;
     }

    public Long getBookingId() { return bookingId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                                // ก้อนเดียวกัน = เท่ากันแน่นอน
        if (!(o instanceof BookingParticipantId other)) return false; // คนละชนิด = ไม่เท่า
        return Objects.equals(bookingId, other.bookingId)          // เทียบค่าทีละช่อง
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, userId);                    // ใช้ช่องเดียวกับใน equals
    }
    
}
