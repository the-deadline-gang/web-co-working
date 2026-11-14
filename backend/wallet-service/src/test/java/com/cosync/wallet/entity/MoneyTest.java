package com.cosync.wallet.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Test
    @DisplayName("TC-MNY-01 เงินเต็มบาทได้ทศนิยม 2 ตำแหน่ง")
    void of_scalesToTwo() {
        assertEquals("240.00", Money.of(new BigDecimal("240")).toPlainString());
        assertEquals("0.01", Money.of(new BigDecimal("0.010")).toPlainString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.00"})
    @DisplayName("TC-MNY-02 ศูนย์หรือติดลบ → ไม่รับ")
    void of_rejectsNonPositive(String amount) {
        assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal(amount)));
    }

    @Test
    @DisplayName("TC-MNY-03 null หรือทศนิยมเกิน 2 ตำแหน่ง → ไม่รับ")
    void of_rejectsNullAndTooPrecise() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal("1.005")));
    }

    @Test
    @DisplayName("TC-MNY-04 แปลงบาทเป็น Token ปัดลง")
    void tokensFor_roundsDown() {
        assertEquals(new BigDecimal("500.00"), Money.tokensFor(500, new BigDecimal("1.0000")));
        assertEquals(new BigDecimal("124.68"), Money.tokensFor(101, new BigDecimal("1.2345")));
        assertEquals(new BigDecimal("0.99"), Money.tokensFor(1, new BigDecimal("0.9999")));
    }
}
