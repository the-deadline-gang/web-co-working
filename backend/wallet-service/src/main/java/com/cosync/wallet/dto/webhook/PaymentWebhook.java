package com.cosync.wallet.dto.webhook;

import java.time.Instant;

/** body ที่ Payment Gateway (หรือ Bruno ตอน mock) ส่งมาที่ POST /webhooks/payment */
public record PaymentWebhook(
        String eventId,       // id ของ event นี้ ใช้กันซ้ำ (BR-05)
        String gatewayRef,    // ตรงกับ topup.gateway_ref
        String status,        // successful / failed
        Integer amountThb,
        Instant paidAt) {

    public boolean isSuccessful() {
        return "successful".equals(status);
    }

    /** ช่องบังคับครบ และ status เป็นค่าที่รู้จัก (ตาราง payment_event มี CHECK ไว้) */
    public boolean isValid() {
        return eventId != null && !eventId.isBlank()
                && gatewayRef != null && !gatewayRef.isBlank()
                && amountThb != null
                && ("successful".equals(status) || "failed".equals(status));
    }
}
