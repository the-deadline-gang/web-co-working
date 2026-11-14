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

class WalletTransactionTest {

    private static WalletTransaction newDebit() {
        return WalletTestData.wallet(1L, 4L, "100.00")
                .debit(TransactionType.BOOKING, new BigDecimal("10"), RefType.BOOKING, 1L, null);
    }

    @Test
    @DisplayName("TC-TXN-01 ใส่ key และ hash ได้ครั้งเดียว")
    void withIdempotency_once() {
        WalletTransaction t = newDebit();

        assertSame(t, t.withIdempotency("k1", "h1"));
        assertEquals("k1", t.getIdempotencyKey());
        assertEquals("h1", t.getRequestHash());
        assertThrows(IllegalStateException.class, () -> t.withIdempotency("k2", "h2"));
    }

    @Test
    @DisplayName("TC-TXN-02 save แล้ว (มี id) → ใส่ key ไม่ได้")
    void withIdempotency_afterSave() {
        WalletTransaction t = WalletTestData.withId(newDebit(), 5L);

        assertThrows(IllegalStateException.class, () -> t.withIdempotency("k", "h"));
    }

    @Test
    @DisplayName("TC-TXN-03 matchesRequest เทียบ hash")
    void matchesRequest() {
        WalletTransaction t = newDebit();
        assertFalse(t.matchesRequest("h1"));          // ยังไม่มี hash (เช่นรายการ TOPUP)

        t.withIdempotency("k1", "h1");
        assertTrue(t.matchesRequest("h1"));
        assertFalse(t.matchesRequest("h2"));
    }
}
