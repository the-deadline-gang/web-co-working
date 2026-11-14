package com.cosync.wallet.entity;

import static com.cosync.wallet.testsupport.WalletTestData.setting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WalletSettingTest {

    @Test
    @DisplayName("TC-SEN-01 allowsAmount รวมขอบ min และ max")
    void allowsAmount() {
        WalletSetting s = setting("1.0000", 100, 10000, 15);

        assertTrue(s.allowsAmount(100));
        assertTrue(s.allowsAmount(10000));
        assertFalse(s.allowsAmount(99));
        assertFalse(s.allowsAmount(10001));
    }

    @Test
    @DisplayName("TC-SEN-02 quickAmounts ตัดยอดที่อยู่นอกช่วงทิ้ง")
    void quickAmounts() {
        assertEquals(List.of(100, 200, 500, 1000, 2000), setting("1.0000", 100, 10000, 15).quickAmounts());
        assertEquals(List.of(200, 500), setting("1.0000", 150, 600, 15).quickAmounts());
        assertEquals(List.of(), setting("1.0000", 3000, 5000, 15).quickAmounts());
    }

    @Test
    @DisplayName("TC-SEN-03 update ค่าใหม่ทั้งหมด / min > max ไม่รับและค่าเดิมไม่เปลี่ยน")
    void update() {
        WalletSetting s = setting("1.0000", 100, 10000, 15);

        s.update(new BigDecimal("1.5000"), 50, 500, 30, "STF-004");
        assertEquals(new BigDecimal("1.5000"), s.getRate());
        assertEquals(50, s.getMinThb());
        assertEquals(500, s.getMaxThb());
        assertEquals(30, s.getQrExpireMinutes());
        assertEquals("STF-004", s.getUpdatedBy());

        assertThrows(IllegalArgumentException.class,
                () -> s.update(new BigDecimal("2"), 600, 500, 10, "STF-005"));
        assertEquals(50, s.getMinThb());
        assertEquals("STF-004", s.getUpdatedBy());
    }
}
