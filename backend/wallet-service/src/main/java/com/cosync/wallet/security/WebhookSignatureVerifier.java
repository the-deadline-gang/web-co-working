package com.cosync.wallet.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cosync.wallet.common.ApiException;

/** ตรวจว่า webhook มาจาก Payment Gateway จริง (BR-04) */
@Component
public class WebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public WebhookSignatureVerifier(@Value("${app.payment.webhook-secret}") String secret) {
        if (secret.isBlank()) {
            throw new IllegalStateException("PAYMENT_WEBHOOK_SECRET ว่าง ตั้งค่าใน .env ก่อน");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * เทียบลายเซ็นใน header กับลายเซ็นที่เราคำนวณเองจาก body + secret
     * ไม่ตรงหรือไม่ส่งมา → 401 ยอดเงินไม่เปลี่ยน
     * @param rawBody body ดิบตามที่ Gateway ส่งมา
     * @param signature ค่าจาก header X-Signature (hex)
     * @throws ApiException 401 INVALID_SIGNATURE
     */
    public void verify(String rawBody, String signature) {
        String expected = sign(rawBody);
        boolean valid = signature != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "ลายเซ็นไม่ถูกต้อง");
        }
    }

    /**
     * เอา body + secret ไปผ่านสูตร HMAC-SHA256 ได้ผลเป็นตัวเลข 32 byte แล้วแปลงเป็นข้อความเลขฐาน 16 ยาว 64 ตัว
     * เป็น package-private เพื่อให้ test สร้างลายเซ็นที่ถูกต้องได้
     * @param body body ดิบ
     * @return ลายเซ็น hex ตัวพิมพ์เล็ก
     */
    String sign(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("สร้าง HMAC ไม่ได้", e);
        }
    }
}
