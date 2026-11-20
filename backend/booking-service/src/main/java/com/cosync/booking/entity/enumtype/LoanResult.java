package com.cosync.booking.entity.enumtype;

public enum LoanResult {
    NORMAL,     // คะแนนลด 0–1  = คืนประกัน 100%
    MINOR,      // คะแนนลด 2–4  = คืน 50%
    MAJOR,      // ลด ≥ 5 หรือใช้งานไม่ได้ = คืน 0%
    LOST    
}
