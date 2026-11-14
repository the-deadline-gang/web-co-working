package com.cosync.wallet.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptPayPayloadTest {

    @Test
    @DisplayName("TC-PP-01 payload ของ 0812345678 ยอด 500 ตรงตามมาตรฐาน EMVCo พร้อม CRC")
    void knownPayload() {
        assertEquals("00020101021229370016A0000006770101110113006681234567853037645406500.005802TH63047190",
                PromptPayPayload.build("0812345678", new BigDecimal("500")));
    }

    @Test
    @DisplayName("TC-PP-02 เบอร์ 0 นำหน้าถูกแปลงเป็น 0066 และยอดมีทศนิยม 2 ตำแหน่ง")
    void phoneAndAmount() {
        String p = PromptPayPayload.build("0899999999", new BigDecimal("1"));

        assertTrue(p.contains("01130066899999999"), p);
        assertTrue(p.contains("54041.00"), p);
        assertTrue(p.matches(".*6304[0-9A-F]{4}$"), p);
    }
}
