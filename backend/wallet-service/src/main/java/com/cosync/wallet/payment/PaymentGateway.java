package com.cosync.wallet.payment;

import java.time.Instant;
import java.util.Optional;

public interface PaymentGateway {

    /** สร้างรายการจ่าย + QR ให้หมดอายุตรงกับ topup ของเรา */
    CreatedPayment createPayment(int amountThb, Instant expiresAt);

    /**
     * ถาม gateway ว่ารายการนี้จบหรือยัง
     * คืน empty ถ้ายังไม่จบ (pending), ถามไม่ได้ หรือ gateway นี้ไม่รองรับการถาม
     */
    Optional<PaymentResult> fetchStatus(String gatewayRef);
}