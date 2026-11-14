package com.cosync.wallet.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cosync.wallet.testsupport.WalletTestData;

class WalletTest {

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("TC-WEN-01 wallet ใหม่ยอด 0.00")
    void newWallet_zero() {
        Wallet w = new Wallet(4L);
        assertEquals(money("0.00"), w.getBalance());
        assertEquals(4L, w.getUserId());
    }

    @Test
    @DisplayName("TC-WEN-02 hasEnough และ shortfallFor")
    void enoughAndShortfall() {
        Wallet w = WalletTestData.wallet(1L, 4L, "60.00");

        assertTrue(w.hasEnough(money("60")));
        assertFalse(w.hasEnough(money("60.01")));
        assertEquals(money("40.00"), w.shortfallFor(money("100")));
        assertEquals(money("0.00"), w.shortfallFor(money("10")));
    }

    @Test
    @DisplayName("TC-WEN-03 credit → ยอดเพิ่ม รายการเป็นบวก")
    void credit() {
        Wallet w = WalletTestData.wallet(1L, 4L, "60.00");

        WalletTransaction t = w.credit(TransactionType.REFUND, money("120"), RefType.BOOKING, 192L, "คืน");

        assertEquals(money("180.00"), w.getBalance());
        assertEquals(money("120.00"), t.getAmount());
        assertEquals(money("180.00"), t.getBalanceAfter());
        assertSame(w, t.getWallet());
        assertEquals(RefType.BOOKING, t.getRefType());
        assertEquals(192L, t.getRefId());
        assertEquals("คืน", t.getNote());
    }

    @Test
    @DisplayName("TC-WEN-04 debit → ยอดลด รายการเป็นลบ")
    void debit() {
        Wallet w = WalletTestData.wallet(1L, 4L, "300.00");

        WalletTransaction t = w.debit(TransactionType.BOOKING, money("240"), RefType.BOOKING, 192L, null);

        assertEquals(money("60.00"), w.getBalance());
        assertEquals(money("-240.00"), t.getAmount());
        assertEquals(money("60.00"), t.getBalanceAfter());
    }

    @Test
    @DisplayName("TC-WEN-05 debit เกินยอด → IllegalStateException ยอดไม่เปลี่ยน")
    void debit_insufficient() {
        Wallet w = WalletTestData.wallet(1L, 4L, "10.00");

        assertThrows(IllegalStateException.class,
                () -> w.debit(TransactionType.BOOKING, money("10.01"), RefType.BOOKING, 1L, null));
        assertEquals(money("10.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-WEN-06 ใช้ type ผิดทาง → IllegalArgumentException")
    void wrongDirection() {
        Wallet w = WalletTestData.wallet(1L, 4L, "100.00");

        assertThrows(IllegalArgumentException.class,
                () -> w.credit(TransactionType.BOOKING, money("1"), RefType.BOOKING, 1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> w.debit(TransactionType.REFUND, money("1"), RefType.BOOKING, 1L, null));
        assertEquals(money("100.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-WEN-07 จำนวนเงินผิดรูปแบบ → ไม่รับ ยอดไม่เปลี่ยน")
    void invalidAmount() {
        Wallet w = WalletTestData.wallet(1L, 4L, "100.00");

        assertThrows(IllegalArgumentException.class,
                () -> w.credit(TransactionType.REFUND, money("-5"), RefType.BOOKING, 1L, null));
        assertEquals(money("100.00"), w.getBalance());
    }
}
