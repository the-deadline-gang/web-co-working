package com.cosync.wallet.entity;

/** ประเภทรายการใน ledger แต่ละประเภทรู้ว่าตัวเองเป็นเงินเข้าหรือเงินออก */
public enum TransactionType {
    TOPUP(true),
    BOOKING(false),
    REFUND(true),
    LOAN_FEE(false),
    DEPOSIT_HOLD(false),
    DEPOSIT_REFUND(true);

    private final boolean credit;

    TransactionType(boolean credit) {
        this.credit = credit;
    }

    /** true = เงินเข้า (amount เป็นบวก), false = เงินออก (amount เป็นลบ) */
    public boolean isCredit() {
        return credit;
    }
}
