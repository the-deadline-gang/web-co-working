package com.cosync.wallet.dto.setting;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletSettingRequest(

        @NotNull(message = "กรุณากรอกอัตราแลกเปลี่ยน")
        @DecimalMin(value = "0.01", message = "อัตราแลกเปลี่ยนต้องไม่ต่ำกว่า 0.01")
        @Digits(integer = 6, fraction = 4, message = "อัตราแลกเปลี่ยนมีทศนิยมได้ไม่เกิน 4 ตำแหน่ง")
        BigDecimal rate,

        @NotNull(message = "กรุณากรอกยอดเติมขั้นต่ำ")
        @Min(value = 1, message = "ยอดเติมขั้นต่ำต้องไม่น้อยกว่า 1 บาท")
        Integer minThb,

        @NotNull(message = "กรุณากรอกยอดเติมสูงสุด")
        @Min(value = 1, message = "ยอดเติมสูงสุดต้องไม่น้อยกว่า 1 บาท")
        Integer maxThb,

        @NotNull(message = "กรุณากรอกอายุ QR")
        @Min(value = 1, message = "อายุ QR ต้องอยู่ระหว่าง 1–60 นาที")
        @Max(value = 60, message = "อายุ QR ต้องอยู่ระหว่าง 1–60 นาที")
        Integer qrExpireMinutes) {}