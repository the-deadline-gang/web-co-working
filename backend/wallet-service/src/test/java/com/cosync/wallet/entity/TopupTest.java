package com.cosync.wallet.entity;

import static com.cosync.wallet.testsupport.WalletTestData.NOW;
import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static com.cosync.wallet.testsupport.WalletTestData.withStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TopupTest {

    @Test
    @DisplayName("TC-TEN-01 สร้างใหม่ → PENDING, userId จาก wallet, Token ตาม rate")
    void newTopup() {
        Topup t = new Topup(wallet(1L, 4L, "0.00"), 500, new BigDecimal("1.2000"), "ref", "qr", NOW, 192L);

        assertEquals(TopupStatus.PENDING, t.getStatus());
        assertEquals(4L, t.getUserId());
        assertEquals(new BigDecimal("600.00"), t.getTokenAmount());
        assertEquals(192L, t.getReturnToBookingId());
        assertTrue(t.isOwnedBy(4L));
        assertFalse(t.isOwnedBy(5L));
    }

    @Test
    @DisplayName("TC-TEN-02 isOverdue: PENDING และเลย expiresAt เท่านั้น")
    void isOverdue() {
        Topup t = topup(7L, wallet(1L, 4L, "0.00"), 500, NOW);

        assertFalse(t.isOverdue(NOW));                       // ตรงเวลาพอดียังไม่หมด
        assertTrue(t.isOverdue(NOW.plusSeconds(1)));
        withStatus(t, TopupStatus.PAID);
        assertFalse(t.isOverdue(NOW.plusSeconds(1)));        // จ่ายแล้วไม่นับว่าหมดอายุ
    }

    @Test
    @DisplayName("TC-TEN-03 markPaid → PAID เติมเงินและผูก transaction")
    void markPaid() {
        Wallet w = wallet(1L, 4L, "100.00");
        Topup t = topup(7L, w, 500, NOW);

        WalletTransaction txn = t.markPaid(NOW);

        assertEquals(TopupStatus.PAID, t.getStatus());
        assertEquals(NOW, t.getPaidAt());
        assertSame(txn, t.getTransaction());
        assertEquals(TransactionType.TOPUP, txn.getType());
        assertEquals(RefType.TOPUP, txn.getRefType());
        assertEquals(7L, txn.getRefId());
        assertEquals("เติมเงิน 500 บาท", txn.getNote());
        assertEquals(new BigDecimal("600.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-TEN-04 EXPIRED ยังจ่ายได้ (Q1 = A)")
    void markPaid_fromExpired() {
        Topup t = withStatus(topup(7L, wallet(1L, 4L, "0.00"), 500, NOW), TopupStatus.EXPIRED);

        assertTrue(t.canBePaid());
        t.markPaid(NOW);
        assertEquals(TopupStatus.PAID, t.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TopupStatus.class, names = {"PAID", "FAILED"})
    @DisplayName("TC-TEN-05 PAID/FAILED จ่ายซ้ำไม่ได้")
    void markPaid_rejected(TopupStatus status) {
        Wallet w = wallet(1L, 4L, "0.00");
        Topup t = withStatus(topup(7L, w, 500, NOW), status);

        assertFalse(t.canBePaid());
        assertThrows(IllegalStateException.class, () -> t.markPaid(NOW));
        assertEquals(new BigDecimal("0.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-TEN-06 markExpired / markFailed เปลี่ยนได้เฉพาะตอน PENDING")
    void markExpiredAndFailed_onlyFromPending() {
        Topup a = topup(1L, wallet(1L, 4L, "0.00"), 500, NOW);
        a.markExpired();
        assertEquals(TopupStatus.EXPIRED, a.getStatus());
        a.markFailed();                                         // EXPIRED แล้ว → ไม่เปลี่ยน
        assertEquals(TopupStatus.EXPIRED, a.getStatus());

        Topup b = topup(2L, wallet(1L, 4L, "0.00"), 500, NOW);
        b.markFailed();
        assertEquals(TopupStatus.FAILED, b.getStatus());

        Topup c = withStatus(topup(3L, wallet(1L, 4L, "0.00"), 500, NOW), TopupStatus.PAID);
        c.markExpired();
        assertEquals(TopupStatus.PAID, c.getStatus());
    }
}
