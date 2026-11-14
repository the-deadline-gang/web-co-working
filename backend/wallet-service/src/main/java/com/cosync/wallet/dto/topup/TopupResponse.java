package com.cosync.wallet.dto.topup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;

public record TopupResponse(
        Long topupId,
        String displayId,
        Integer amountThb,
        BigDecimal tokenAmount,
        TopupStatus status,
        String qrImage,
        Instant expiresAt,
        Instant paidAt,
        BigDecimal balanceAfter,
        Long returnToBookingId) {

    // วันที่ใน displayId ใช้เวลาไทย ให้ตรงกับที่ลูกค้าเห็นบนสลิป
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Bangkok"));

    /** ต้องเรียกภายใน @Transactional เพราะอ่าน transaction (LAZY) ตอน PAID */
    public static TopupResponse from(Topup topup) {
        boolean pending = topup.isPending();
        boolean paid = topup.getStatus() == TopupStatus.PAID;

        return new TopupResponse(
                topup.getId(),
                "PAY-%s-%04d".formatted(DATE.format(topup.getCreatedAt()), topup.getId()),
                topup.getAmountThb(),
                topup.getTokenAmount(),
                topup.getStatus(),
                pending ? topup.getQrImage() : null,
                topup.getExpiresAt(),
                topup.getPaidAt(),
                paid ? topup.getTransaction().getBalanceAfter() : null,
                topup.getReturnToBookingId());
    }
}
