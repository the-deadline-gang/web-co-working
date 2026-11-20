package com.cosync.booking.entity.enumtype;

public enum EquipmentStatus {
    AVAILABLE,      // ว่าง พร้อมให้ยืม
    BORROWED,       // มีคนยืมอยู่
    MAINTENANCE,    // ส่งซ่อม (คืนแบบเสียหายหนัก หรือ IT ส่งเอง)
    LOST            // ยืมไปแล้วไม่คืนครบ 3 วัน (BR-33)
}