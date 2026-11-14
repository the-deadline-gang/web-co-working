package com.cosync.wallet.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cosync.wallet.dto.wallet.MoneyMoveRequest;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;

class MoneyMoveRequestTest {

    private static MoneyMoveRequest req(String amount, String note) {
        return new MoneyMoveRequest(new BigDecimal(amount), TransactionType.BOOKING, RefType.BOOKING, 192L, note);
    }

    @Test
    @DisplayName("TC-HSH-01 hash = SHA-256 ของ userId|direction|amount|type|refType|refId|note")
    void knownValue() {
        // echo -n '4|debit|240.00|BOOKING|BOOKING|192|' | shasum -a 256
        assertEquals("111524dcaa5fe5990fbd8a3124257babe9df2469ca0547236e6e98a2b4e67864",
                req("240", null).requestHash(4L, "debit"));
    }

    @Test
    @DisplayName("TC-HSH-02 240 กับ 240.00 ได้ hash เดียวกัน, note null เท่ากับ note ว่าง")
    void canonicalAmountAndNote() {
        assertEquals(req("240", null).requestHash(4L, "debit"), req("240.00", "").requestHash(4L, "debit"));
    }

    @Test
    @DisplayName("TC-HSH-03 คนละ user / คนละทาง / คนละยอด → hash ต่าง")
    void differentInputs() {
        String base = req("240", null).requestHash(4L, "debit");

        assertNotEquals(base, req("240", null).requestHash(5L, "debit"));
        assertNotEquals(base, req("240", null).requestHash(4L, "credit"));
        assertNotEquals(base, req("240.01", null).requestHash(4L, "debit"));
        assertNotEquals(base, req("240", "x").requestHash(4L, "debit"));
    }
}
