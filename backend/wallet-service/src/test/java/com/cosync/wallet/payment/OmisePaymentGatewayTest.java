package com.cosync.wallet.payment;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** ใช้ MockRestServiceServer แทน api.omise.co ไม่มีการยิงออกเน็ตจริง */
class OmisePaymentGatewayTest {

    private static final String KEY = "skey_test_abc";
    private static final String CHARGE_URL = "https://api.omise.co/charges/chrg_test_1";
    private static final String QR_URL = "https://api.omise.co/charges/chrg_test_1/documents/docu_test_1/downloads/QR";

    private MockRestServiceServer omise;
    private OmisePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        omise = MockRestServiceServer.bindTo(builder).build();     // ต้อง bind ก่อน gateway เรียก build()
        gateway = new OmisePaymentGateway(builder, KEY);
    }

    /** หน้าตา charge ที่ Omise ตอบกลับ (ตัดให้เหลือ field ที่เราใช้ + field ที่ไม่รู้จักไว้เช็ก ignoreUnknown) */
    private static String charge(String status, String paidAt) {
        return """
                {"object": "charge", "id": "chrg_test_1", "status": "%s", "paid": %s,
                 "amount": 50000, "currency": "THB", "paid_at": %s,
                 "source": {"type": "promptpay",
                            "scannable_code": {"image": {"download_uri": "%s"}}}}
                """.formatted(status, paidAt != null, paidAt == null ? "null" : "\"" + paidAt + "\"", QR_URL);
    }

    // ===================== createPayment =====================

    @Test
    @DisplayName("TC-OMI-01 สร้าง charge: ส่งสตางค์, THB, promptpay, expires_at (ตัดเศษวินาที) และ Basic auth ด้วย secret key")
    void createPayment_sendsCorrectRequest() {
        String basic = "Basic " + Base64.getEncoder().encodeToString((KEY + ":").getBytes(StandardCharsets.UTF_8));
        omise.expect(requestTo("https://api.omise.co/charges"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", basic))
                .andExpect(content().formDataContains(Map.of(
                        "amount", "50000",
                        "currency", "THB",
                        "source[type]", "promptpay",
                        "expires_at", "2026-11-14T05:15:00Z")))
                .andRespond(withSuccess(charge("pending", null), MediaType.APPLICATION_JSON));

        CreatedPayment p = gateway.createPayment(500, Instant.parse("2026-11-14T05:15:00.987654Z"));

        assertEquals("chrg_test_1", p.gatewayRef());
        assertEquals(QR_URL, p.qrImage());
        omise.verify();
    }

    @Test
    @DisplayName("TC-OMI-02 Omise ล่ม → 502 PAYMENT_GATEWAY_UNAVAILABLE")
    void createPayment_omiseDown() {
        omise.expect(requestTo("https://api.omise.co/charges")).andRespond(withServerError());

        assertApiError(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_UNAVAILABLE",
                () -> gateway.createPayment(500, Instant.now()));
    }

    // ===================== fetchStatus =====================

    @Test
    @DisplayName("TC-OMI-03 charge จ่ายแล้ว → successful, แปลงสตางค์เป็นบาท, มี paidAt")
    void fetchStatus_successful() {
        omise.expect(requestTo(CHARGE_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(charge("successful", "2026-11-14T05:03:00Z"), MediaType.APPLICATION_JSON));

        PaymentResult r = gateway.fetchStatus("chrg_test_1").orElseThrow();

        assertEquals("chrg_test_1", r.gatewayRef());
        assertTrue(r.isSuccessful());
        assertEquals(500, r.amountThb());
        assertEquals(Instant.parse("2026-11-14T05:03:00Z"), r.paidAt());
        omise.verify();
    }

    @Test
    @DisplayName("TC-OMI-04 charge failed → failed ไม่มี paidAt")
    void fetchStatus_failed() {
        omise.expect(requestTo(CHARGE_URL)).andRespond(withSuccess(charge("failed", null), MediaType.APPLICATION_JSON));

        PaymentResult r = gateway.fetchStatus("chrg_test_1").orElseThrow();

        assertEquals("failed", r.status());
        assertNull(r.paidAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "expired", "reversed"})
    @DisplayName("TC-OMI-05 ยังไม่จบ (pending/expired/reversed) → empty")
    void fetchStatus_notFinal(String status) {
        omise.expect(requestTo(CHARGE_URL)).andRespond(withSuccess(charge(status, null), MediaType.APPLICATION_JSON));

        assertEquals(Optional.empty(), gateway.fetchStatus("chrg_test_1"));
    }

    @Test
    @DisplayName("TC-OMI-06 Omise ล่ม หรือไม่รู้จัก charge → empty ไม่โยน exception")
    void fetchStatus_errors() {
        omise.expect(requestTo(CHARGE_URL)).andRespond(withServerError());
        assertEquals(Optional.empty(), gateway.fetchStatus("chrg_test_1"));

        omise.reset();
        omise.expect(requestTo("https://api.omise.co/charges/chrg_test_nope")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertEquals(Optional.empty(), gateway.fetchStatus("chrg_test_nope"));
    }

    // ===================== กัน live key =====================

    @ParameterizedTest
    @ValueSource(strings = {"skey_live_abc", "pkey_test_abc", ""})
    @DisplayName("TC-OMI-07 ไม่ใช่ secret test key → start ไม่ได้")
    void rejectsNonTestKey(String key) {
        assertThrows(IllegalStateException.class, () -> new OmisePaymentGateway(RestClient.builder(), key));
    }

    @Test
    @DisplayName("TC-OMI-08 ไม่ได้ตั้ง key (null) → start ไม่ได้ / constructor ที่ Spring ใช้รับ test key ได้")
    void nullKeyAndSpringConstructor() {
        assertThrows(IllegalStateException.class, () -> new OmisePaymentGateway(RestClient.builder(), null));
        assertDoesNotThrow(() -> new OmisePaymentGateway(KEY));
    }
}
