package com.cosync.booking.entity.enumtype;

public enum LoanStatus {
    BORROWED,   // ยืมอยู่
    RETURNED,   // คืนแล้ว
    OVERDUE,    // ปิดทำการแล้วยังไม่คืน (BR-33)
    LOST        // OVERDUE ครบ 3 วัน ถือว่าหาย
}
