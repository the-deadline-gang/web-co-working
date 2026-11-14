package com.cosync.wallet.payment;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON ที่ Omise ตอบกลับจาก POST /charges และ GET /charges/{id}
 * เก็บแค่ช่องที่ใช้ ช่องอื่นไม่สนใจ (ignoreUnknown)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OmiseCharge(
        String id,                                   // chrg_test_... → topup.gateway_ref
        String status,                               // pending / successful / failed / expired
        boolean paid,
        long amount,                                 // หน่วยสตางค์
        @JsonProperty("paid_at") Instant paidAt,
        Source source) {

    /** URL รูป QR ใส่ <img src> ได้เลย */
    String qrUrl() {
        return source.scannableCode().image().downloadUri();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Source(@JsonProperty("scannable_code") ScannableCode scannableCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScannableCode(Image image) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Image(@JsonProperty("download_uri") String downloadUri) {}
}
