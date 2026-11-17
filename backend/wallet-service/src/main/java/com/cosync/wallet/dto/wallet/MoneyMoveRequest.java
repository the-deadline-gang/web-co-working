package com.cosync.wallet.dto.wallet;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** body ของ POST /internal/wallets/{userId}/debit และ /credit (booking-service ส่งมา) */
public record MoneyMoveRequest(

        @NotNull(message = "กรุณาระบุจำนวนเงิน")
        @DecimalMin(value = "0.01", message = "จำนวนเงินต้องมากกว่า 0")
        @Digits(integer = 10, fraction = 2, message = "จำนวนเงินมีทศนิยมได้ไม่เกิน 2 ตำแหน่ง")
        BigDecimal amount,            // ส่งเป็นค่าบวกเสมอ ระบบใส่เครื่องหมายเองตาม endpoint

        @NotNull(message = "กรุณาระบุประเภทรายการ")
        TransactionType type,

        @NotNull(message = "กรุณาระบุ refType")
        RefType refType,

        @NotNull(message = "กรุณาระบุ refId")
        @Positive(message = "refId ต้องมากกว่า 0")
        Long refId,

        @Size(max = 200, message = "note ยาวได้ไม่เกิน 200 ตัวอักษร")
        String note) {

    /**
     * ลายนิ้วมือ (SHA-256) ของคำขอนี้ ไว้เช็กว่า Idempotency-Key เดิมมากับ body เดิมไหม
     * คำนวณจากค่าแต่ละช่อง ไม่ใช่ JSON ดิบ เพราะ JSON ที่เว้นวรรคต่างกันต้องนับว่าเป็นคำขอเดียวกัน
     * ใส่ userId กับ direction ด้วย key เดิมไปใช้กับคนอื่นหรือคนละทางจะได้ 409 ไม่ใช่ได้ผลของคำขออื่นกลับไป
     * hash ย้อนกลับเป็นข้อมูลเดิมไม่ได้ ใช้เทียบเท่านั้น (ไม่ใช่การเข้ารหัส)
     * @param userId    เจ้าของ wallet
     * @param direction "debit" หรือ "credit"
     * @return SHA-256 เป็นตัวอักษร hex 64 ตัว
     */
    public String requestHash(Long userId, String direction) {
        String canonical = String.join("|",
                String.valueOf(userId),
                direction,
                amount.setScale(2).toPlainString(),      // 240 กับ 240.00 นับเป็นค่าเดียวกัน
                type.name(),
                refType.name(),
                String.valueOf(refId),
                note == null ? "" : note);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("ไม่มี SHA-256", e);
        }
    }
}
