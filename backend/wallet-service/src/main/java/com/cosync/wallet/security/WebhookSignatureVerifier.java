package com.cosync.wallet.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

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
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** ลายเซ็นไม่ตรง (หรือไม่ส่งมา) → 401 ยอดไม่เปลี่ยน */
    /**
     * เอาไว้ยืนยันลายเช็นว่าที่ส่งมากับที่มีอยู่มันตรงกันไหม โดยที่เอา  signature จาก header มาแล้วก็เอา เอา body ที่ได้รับ + secret ของเรา แล้วก็เอามาเปรียบเทียบ
     * @param rawBody ข้อมูลดิบทั้งก้อน
     * @param signature  signature จาก header
     */
    public void verify(String rawBody, String signature) {
        String expected = sign(rawBody);
        boolean valid = signature != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "ลายเซ็นไม่ถูกต้อง");
        }
    }

    /**
     * เอา body + secret ไปผ่านสูตร HMAC-SHA256 ได้ผลเป็นตัวเลข 32 byte แล้วแปลงเป็นข้อความเลขฐาน 16 ยาว 64 ตัว
     * @param body ข้อมูลดิบทั้งก้อน
     * @return ส่งกลับไปเป็นรหัส  signature
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
