package com.cosync.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.wallet.dto.webhook.PaymentWebhook;
import com.cosync.wallet.security.WebhookSignatureVerifier;
import com.cosync.wallet.service.PaymentWebhookService;

@WebMvcTest(WebhookController.class)
@Import(WebhookSignatureVerifier.class)
@TestPropertySource(properties = {
        "app.internal.token=test-internal-token",
        "app.payment.webhook-secret=test-secret"})
class WebhookControllerTest {

    private static final String BODY = """
            {"eventId":"evt_1","gatewayRef":"mock_ref_7","status":"successful","amountThb":500,"paidAt":"2026-11-14T05:00:00Z"}""";

    @Autowired MockMvc mockMvc;

    @MockitoBean PaymentWebhookService paymentWebhookService;

    /** คำนวณลายเซ็นแบบเดียวกับที่ gateway (หรือ Bruno) ทำ */
    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("TC-WH-H01 ลายเซ็นถูก → 200 และส่ง body ดิบไปให้ service")
    void validSignature() throws Exception {
        mockMvc.perform(post("/webhooks/payment").header("X-Signature", sign(BODY))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        verify(paymentWebhookService).handle(any(PaymentWebhook.class), eq(BODY));
    }

    @Test
    @DisplayName("TC-WH-H02 ไม่มี / ผิดลายเซ็น → 401 ไม่ถึง service")
    void invalidSignature() throws Exception {
        mockMvc.perform(post("/webhooks/payment").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));
        mockMvc.perform(post("/webhooks/payment").header("X-Signature", "deadbeef")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentWebhookService);
    }

    @Test
    @DisplayName("TC-WH-H03 ลายเซ็นถูกแต่ JSON พัง / ขาดช่อง / status แปลก → 400")
    void invalidPayload() throws Exception {
        for (String body : new String[] {
                "not-json",
                "{\"eventId\":\"evt_1\"}",
                "{\"eventId\":\"evt_1\",\"gatewayRef\":\"r\",\"status\":\"pending\",\"amountThb\":500}"}) {
            mockMvc.perform(post("/webhooks/payment").header("X-Signature", sign(body))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(paymentWebhookService);
    }

    @Test
    @DisplayName("TC-WH-H04 event เดียวกันมาพร้อมกันจน PK ชน → ยังตอบ 200 (gateway จะได้ไม่ส่งซ้ำ)")
    void concurrentDuplicate_returnsOk() throws Exception {
        doThrow(new DataIntegrityViolationException("Duplicate entry 'evt_1' for key 'PRIMARY'"))
                .when(paymentWebhookService).handle(any(), any());

        mockMvc.perform(post("/webhooks/payment").header("X-Signature", sign(BODY))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }
}
