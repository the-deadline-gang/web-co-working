package com.cosync.booking.dto.resource;

import java.math.BigDecimal;

import com.cosync.booking.entity.enumtype.ServiceType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ข้อมูล resource ที่ใช้ทั้งตอนสร้าง (POST) และแก้ (PUT) ตาม ResourceInput ใน spec
 * กฎที่ต้องดู 2 ช่องพร้อมกัน (DAY_PASS ต้องมี dailyQuota / ตัวอื่นห้ามมี) ตรวจใน ResourceService
 */
public record ResourceRequest(

        @NotBlank(message = "กรุณากรอกรหัส resource")
        @Size(max = 20, message = "รหัส resource ยาวได้ไม่เกิน {max} ตัวอักษร")
        String code,

        @NotNull(message = "กรุณาเลือกโซน")
        Long zoneId,

        @NotNull(message = "กรุณาเลือกประเภทบริการ")
        ServiceType serviceType,

        @NotNull(message = "กรุณากรอกจำนวนคนที่รองรับได้")
        @Min(value = 1, message = "ต้องรองรับได้อย่างน้อย {value} คน")
        Integer capacity,

        /** มีเฉพาะ DAY_PASS ห้อง/โต๊ะส่ง null (ไม่ส่งมาเลย) */
        @Min(value = 1, message = "โควตารายวันต้องไม่น้อยกว่า {value}")
        Integer dailyQuota,

        @DecimalMin(value = "0.01", message = "ขนาดพื้นที่ต้องมากกว่า 0")
        @Digits(integer = 4, fraction = 2, message = "ขนาดพื้นที่ต้องไม่เกิน 9999.99 ตร.ม. และทศนิยมไม่เกิน 2 ตำแหน่ง")
        BigDecimal sizeSqm,

        @Size(max = 500, message = "ลิงก์รูปยาวได้ไม่เกิน {max} ตัวอักษร")
        String imageUrl,

        /** ตอน POST ไม่ใช้ (สร้างมาเปิดเสมอ) · ตอน PUT ถ้าเป็น null = ไม่เปลี่ยน */
        Boolean isActive) {
}
