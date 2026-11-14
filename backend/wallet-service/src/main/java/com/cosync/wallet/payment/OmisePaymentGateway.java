package com.cosync.wallet.payment;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cosync.wallet.common.ApiException;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "omise")
public class OmisePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(OmisePaymentGateway.class);

    private final RestClient omise;

    // ⓐ Spring ใช้ตัวนี้
   @Autowired
    public OmisePaymentGateway(@Value("${app.payment.omise.secret-key}") String secretKey) {
        this(defaultBuilder(), secretKey);
    }

    private static RestClient.Builder defaultBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory);
    }

    // ⓐ test ใช้ตัวนี้ ส่ง builder ที่ผูกกับ server ปลอมเข้ามา
    OmisePaymentGateway(RestClient.Builder builder, String secretKey) {
        // ⓑ กันการเผลอใส่ live key (skey_...) ซึ่งเป็นเงินจริง
        if (secretKey == null || !secretKey.startsWith("skey_test_")) {
            throw new IllegalStateException("OMISE_SECRET_KEY ต้องเป็น test key (skey_test_...) เท่านั้น");
        }
        this.omise = builder
                .baseUrl("https://api.omise.co")
                .defaultHeaders(h -> h.setBasicAuth(secretKey, ""))
                .build();
    }

    @Override
    public CreatedPayment createPayment(int amountThb, Instant expiresAt) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", String.valueOf(amountThb * 100));             // บาท → สตางค์
        form.add("currency", "THB");
        form.add("source[type]", "promptpay");
        form.add("expires_at", expiresAt.truncatedTo(ChronoUnit.SECONDS).toString());   // ⓒ เช่น 2026-11-14T07:15:00Z
        try {
            OmiseCharge charge = omise.post().uri("/charges")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OmiseCharge.class);
            return new CreatedPayment(charge.id(), charge.qrUrl());
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_UNAVAILABLE",
                    "ระบบชำระเงินขัดข้อง กรุณาลองใหม่");
        }
    }

    // ⓓ
    @Override
    public Optional<PaymentResult> fetchStatus(String gatewayRef) {
        try {
            OmiseCharge charge = omise.get().uri("/charges/{id}", gatewayRef)
                    .retrieve()
                    .body(OmiseCharge.class);
            return toResult(charge);
        } catch (RestClientException e) {
            // ถามไม่ได้ก็แค่ยังไม่รู้ผล ไม่ทำให้ GET /topups/{id} ของสมาชิกพัง เดี๋ยว poll รอบหน้าถามใหม่
            log.warn("ถามสถานะ charge {} จาก Omise ไม่ได้: {}", gatewayRef, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<PaymentResult> toResult(OmiseCharge c) {
        int amountThb = Math.toIntExact(c.amount() / 100);                // สตางค์ → บาท
        return switch (String.valueOf(c.status())) {
            case "successful" -> Optional.of(new PaymentResult(c.id(), "successful", amountThb, c.paidAt()));
            case "failed"     -> Optional.of(new PaymentResult(c.id(), "failed", amountThb, null));
            default           -> Optional.empty();   // pending / expired / reversed ยังไม่ถือว่าจบ ฝั่งเรามี TopupExpiryJob ดูแลเรื่องหมดอายุเอง
        };
    }
}