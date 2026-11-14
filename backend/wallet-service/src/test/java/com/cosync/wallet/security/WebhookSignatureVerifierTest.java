package com.cosync.wallet.security;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WebhookSignatureVerifierTest {

    private static final String BODY = "{\"eventId\":\"evt_1\"}";
    // HMAC-SHA256(key="test-secret", BODY)
    private static final String SIG = "9e048689e66ae6f6c280b6e41dcb39dc11e153ce8e20b57762470a26ae76b817";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("test-secret");

    @Test
    @DisplayName("TC-SIG-01 sign() ตรงกับค่ามาตรฐานของ HMAC-SHA256")
    void sign_knownVector() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                new WebhookSignatureVerifier("key").sign("The quick brown fox jumps over the lazy dog"));
        assertEquals(SIG, verifier.sign(BODY));
    }

    @Test
    @DisplayName("TC-SIG-02 ลายเซ็นถูก (ตัวเล็กหรือตัวใหญ่) → ผ่าน")
    void verify_ok() {
        assertDoesNotThrow(() -> verifier.verify(BODY, SIG));
        assertDoesNotThrow(() -> verifier.verify(BODY, SIG.toUpperCase()));
    }

    @Test
    @DisplayName("TC-SIG-03 ไม่มีลายเซ็น / ผิด / body ถูกแก้ / secret ไม่ตรง → 401")
    void verify_rejected() {
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", () -> verifier.verify(BODY, null));
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", () -> verifier.verify(BODY, "abc"));
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE",
                () -> verifier.verify(BODY.replace("evt_1", "evt_2"), SIG));
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE",
                () -> new WebhookSignatureVerifier("other-secret").verify(BODY, SIG));
    }
}
