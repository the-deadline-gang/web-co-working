package com.cosync.booking.dto.rate;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** แก้ราคาและส่วนลดของบริการ 1 ประเภท (PUT /admin/rates/{serviceType}) */
public record RateUpdateRequest(

        @NotNull(message = "กรุณากรอกราคา")
        @DecimalMin(value = "0.01", message = "ราคาต้องมากกว่า 0")
        @Digits(integer = 10, fraction = 2, message = "ราคามีทศนิยมได้ไม่เกิน 2 ตำแหน่ง")
        BigDecimal rate,

        @NotNull(message = "กรุณากรอกส่วนลด (ไม่มีส่วนลดให้ใส่ 0)")
        @DecimalMin(value = "0", message = "ส่วนลดต้องไม่ติดลบ")
        @DecimalMax(value = "100", message = "ส่วนลดต้องไม่เกิน 100%")
        @Digits(integer = 3, fraction = 2, message = "ส่วนลดมีทศนิยมได้ไม่เกิน 2 ตำแหน่ง")
        BigDecimal discountPct
) {}
