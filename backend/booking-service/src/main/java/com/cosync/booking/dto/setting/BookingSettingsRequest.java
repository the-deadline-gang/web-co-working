package com.cosync.booking.dto.setting;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * ค่าที่ admin ส่งมาแก้ค่าตั้งของการจองและยืม-คืน (PUT /admin/settings)
 * เวลาเปิด-ปิดเป็นเวลาไทย รับรูปแบบ "08:00"
 * กฎที่เทียบ 2 ช่อง (เปิดก่อนปิด, 50% น้อยกว่า 100%) เช็กใน service แล้วตอบ 400
 */
public record BookingSettingsRequest(

        @NotNull(message = "กรุณากรอกเวลาเปิด")
        LocalTime openTime,

        @NotNull(message = "กรุณากรอกเวลาปิด")
        LocalTime closeTime,

        @NotNull(message = "กรุณากรอกจำนวนวันที่จองล่วงหน้าได้")
        @Min(value = 1, message = "จองล่วงหน้าได้อย่างน้อย 1 วัน")
        @Max(value = 365, message = "จองล่วงหน้าได้ไม่เกิน 365 วัน")
        Integer bookAheadDays,

        @NotNull(message = "กรุณากรอกเวลาล็อกที่")
        @Min(value = 1, message = "เวลาล็อกที่ต้องไม่น้อยกว่า {value} นาที")
        @Max(value = 60, message = "เวลาล็อกที่ต้องไม่เกิน {value} นาที")
        Integer holdMinutes,

        @NotNull(message = "กรุณากรอกเวลาต่อการล็อก")
        @Min(value = 1, message = "เวลาต่อการล็อกต้องไม่น้อยกว่า {value} นาที")
        @Max(value = 60, message = "เวลาต่อการล็อกต้องไม่เกิน {value} นาที")
        Integer holdExtendMinutes,

        @NotNull(message = "กรุณากรอกเวลาที่เข้าได้ก่อนเริ่ม")
        @Min(value = 0, message = "เวลาที่เข้าได้ก่อนเริ่มต้องไม่ติดลบ")
        @Max(value = 120, message = "เวลาที่เข้าได้ก่อนเริ่มต้องไม่เกิน {value} นาที")
        Integer earlyCheckinMinutes,

        @NotNull(message = "กรุณากรอกเวลา no-show")
        @Min(value = 1, message = "เวลา no-show ต้องไม่น้อยกว่า {value} นาที")
        @Max(value = 240, message = "เวลา no-show ต้องไม่เกิน {value} นาที")
        Integer noShowMinutes,

        @NotNull(message = "กรุณากรอกชั่วโมงที่ยกเลิกแล้วได้คืน 100%")
        @Min(value = 1, message = "ชั่วโมงที่ได้คืน 100% ต้องไม่น้อยกว่า {value}")
        Integer cancelFullRefundHours,

        @NotNull(message = "กรุณากรอกชั่วโมงที่ยกเลิกแล้วได้คืน 50%")
        @Min(value = 0, message = "ชั่วโมงที่ได้คืน 50% ต้องไม่ติดลบ")
        Integer cancelHalfRefundHours,

        @NotNull(message = "กรุณากรอกจำนวนอุปกรณ์ที่ยืมได้")
        @Min(value = 1, message = "ยืมได้อย่างน้อย {value} ชิ้น")
        @Max(value = 20, message = "ยืมได้ไม่เกิน {value} ชิ้น")
        Integer loanMaxItems,

        @NotNull(message = "กรุณากรอกจำนวนวันก่อนถือว่าหาย")
        @Min(value = 1, message = "จำนวนวันก่อนถือว่าหายต้องไม่น้อยกว่า {value} วัน")
        Integer lostAfterDays) {
}
