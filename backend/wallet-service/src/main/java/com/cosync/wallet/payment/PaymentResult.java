package com.cosync.wallet.payment;

import java.time.Instant;

/** ผลการจ่ายที่ได้จากการถาม gateway (มีเฉพาะสถานะที่จบแล้ว: successful / failed) */
public record PaymentResult(
        String gatewayRef,
        String status,        // "successful" หรือ "failed" ใช้คำเดียวกับ PaymentWebhook
        int amountThb,
        Instant paidAt) {

    public boolean isSuccessful() {
        return "successful".equals(status);
    }
}