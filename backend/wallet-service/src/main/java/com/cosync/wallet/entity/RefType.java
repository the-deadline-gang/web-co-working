package com.cosync.wallet.entity;

/** รายการต้นทางที่ทำให้เงินเคลื่อน (ref_type ใน wallet_transaction) */
public enum RefType {
    TOPUP,
    BOOKING,
    LOAN
}
