package com.cosync.wallet.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.webhook.PaymentWebhook;
import com.cosync.wallet.security.WebhookSignatureVerifier;
import com.cosync.wallet.service.PaymentWebhookService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSignatureVerifier signatureVerifier;
    private final PaymentWebhookService paymentWebhookService;
    private final JsonMapper jsonMapper;

    public WebhookController(WebhookSignatureVerifier signatureVerifier,
                             PaymentWebhookService paymentWebhookService,
                             JsonMapper jsonMapper) {
        this.signatureVerifier = signatureVerifier;
        this.paymentWebhookService = paymentWebhookService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * รับเป็น String ดิบ ไม่ใช่ PaymentWebhook เพราะลายเซ็นคำนวณจาก byte ที่ส่งมาเป๊ะๆ
     * ถ้าให้ Spring แปลงเป็น object ก่อน ช่องว่างหรือลำดับ field อาจเปลี่ยน ลายเซ็นจะไม่ตรง
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> payment(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        signatureVerifier.verify(rawBody, signature);
        
        PaymentWebhook event = parse(rawBody);

        // 3. ทำงานจริง
        try {
            paymentWebhookService.handle(event, rawBody);
        } catch (DataIntegrityViolationException duplicate) {
            // event เดียวกันมาพร้อมกัน 2 ตัว ตัวที่ช้ากว่าชน PK ของ payment_event อีกตัวจัดการไปแล้ว
            log.info("webhook {} มาพร้อมกันซ้ำ ข้าม", event.eventId());
        }

        // 4. ตอบ 200 เสมอเมื่อลายเซ็นถูก Gateway จะได้ไม่ยิงซ้ำ
        return ResponseEntity.ok().build();
    }

    private PaymentWebhook parse(String rawBody) {
        try {
            PaymentWebhook event = jsonMapper.readValue(rawBody, PaymentWebhook.class);
            if (event != null && event.isValid()) {
                return event;
            }
        } catch (JacksonException e) {
            // ตกลงไปตอบ 400 ด้านล่าง
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "รูปแบบ webhook ไม่ถูกต้อง");
    }
}
