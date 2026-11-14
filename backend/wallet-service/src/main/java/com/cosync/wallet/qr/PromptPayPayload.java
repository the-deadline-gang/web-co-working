package com.cosync.wallet.qr;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;


public final class PromptPayPayload {

    private PromptPayPayload() {}

    /** @param promptPayPhone เบอร์ร้าน เช่น 0812345678 */
    public static String build(String promptPayPhone, BigDecimal amount) {
        String account = field("00", "A000000677010111")
                       + field("01", "0066" + promptPayPhone.substring(1));   // 08x → 00668x

        String body = field("00", "01")
                    + field("01", "12")
                    + field("29", account)
                    + field("53", "764")
                    + field("54", amount.setScale(2).toPlainString())
                    + field("58", "TH")
                    + "6304";                                    // ใส่หัวช่อง CRC ไว้ก่อน แล้วคำนวณรวมด้วย
        return body + crc16(body);
    }

    private static String field(String id, String value) {
        return id + "%02d".formatted(value.length()) + value;
    }

    /** CRC-16/CCITT-FALSE ตามมาตรฐาน EMVCo */
    private static String crc16(String s) {
        int crc = 0xFFFF;
        for (byte b : s.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return "%04X".formatted(crc & 0xFFFF);
    }
}
