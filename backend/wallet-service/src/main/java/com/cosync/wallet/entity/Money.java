package com.cosync.wallet.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** ตัวช่วยเรื่องจำนวนเงิน: ทศนิยม 2 ตำแหน่งเสมอ และต้องมากกว่า 0 */
public final class Money {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    /** ตรวจว่าเป็นจำนวนบวกที่มีทศนิยมไม่เกิน 2 ตำแหน่ง แล้วคืนค่าที่มี scale 2 */
    public static BigDecimal of(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("จำนวนเงินต้องมากกว่า 0");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("จำนวนเงินมีทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** บาท x rate แล้วปัดลงที่ 2 ตำแหน่ง ไม่ให้ได้ Token เกินเงินที่จ่าย (Q3) */
    public static BigDecimal tokensFor(int amountThb, BigDecimal rate) {
        return BigDecimal.valueOf(amountThb).multiply(rate).setScale(2, RoundingMode.DOWN);
    }
}
