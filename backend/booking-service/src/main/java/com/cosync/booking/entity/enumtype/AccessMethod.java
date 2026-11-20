package com.cosync.booking.entity.enumtype;

/** การเข้า-ออกครั้งนั้นเกิดขึ้นด้วยวิธีไหน */
public enum AccessMethod {
    QR,         // ผู้ใช้สแกนที่ Kiosk เอง (F7, F10)
    OVERRIDE,   // พนักงานอนุญาตให้ออกทั้งที่มีของค้าง (BR-41)
    AUTO        // ระบบ check-out ให้ตอนปิดทำการ (BR-24)
}