package com.cosync.wallet.payment;

public interface PaymentGateway {
    /** สร้างรายการรอจ่าย คืน ref ไว้จับคู่ตอน webhook และรูป QR */
    CreatedPayment createPayment(int amountThb);
}