package com.cosync.wallet.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.cosync.wallet.security.MemberOnly;
import com.cosync.wallet.security.RequirePermission;
import com.cosync.wallet.security.StaffOnly;
import com.cosync.wallet.security.WebhookSignatureVerifier;
import com.cosync.wallet.service.PaymentPoller;
import com.cosync.wallet.service.PaymentWebhookService;
import com.cosync.wallet.service.ReportService;
import com.cosync.wallet.service.TopupService;
import com.cosync.wallet.service.WalletService;

/**
 * ตารางสิทธิ์ของทุก endpoint ใน wallet-service (ยิงผ่าน MockMvc จริง ผ่าน interceptor จริง)
 * service ทั้งหมดเป็น mock: ถ้าโดนปฏิเสธต้องไม่ถึง service เลย
 *
 * | endpoint                       | ใครเข้าได้                    |
 * |--------------------------------|-----------------------------|
 * | /me/wallet/**, /topups (POST/GET id) | MEMBER                  |
 * | /admin/topups, /admin/reports/** | STAFF + report.view        |
 * | /admin/wallet-settings (GET/PUT) | STAFF + setting.manage     |
 * | /internal/**                   | X-Internal-Token ถูกต้อง       |
 * | /topups/config                 | ทุกคน                        |
 * | /webhooks/payment              | ทุกคน แต่ต้องมีลายเซ็น (ทดสอบใน WebhookControllerTest) |
 */
@WebMvcTest({WalletController.class, TopupController.class, AdminReportController.class,
        AdminWalletSettingController.class, InternalWalletController.class, WebhookController.class})
@Import(WebhookSignatureVerifier.class)
@TestPropertySource(properties = {
        "app.internal.token=" + EndpointSecurityTest.TOKEN,
        "app.payment.webhook-secret=test-secret"})
class EndpointSecurityTest {

    static final String TOKEN = "test-internal-token";
    private static final String ALL_PERMS = "report.view,setting.manage,rate.manage";

    @Autowired MockMvc mockMvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping;

    @MockitoBean WalletService walletService;
    @MockitoBean TopupService topupService;
    @MockitoBean ReportService reportService;
    @MockitoBean PaymentPoller paymentPoller;
    @MockitoBean PaymentWebhookService paymentWebhookService;

    // ------------------------------------------------------------------ helpers

    /** request ที่ "ถูกต้องทุกอย่างยกเว้นเรื่องสิทธิ์" จะได้รู้ว่าที่โดนปฏิเสธเป็นเพราะสิทธิ์จริงๆ */
    private static MockHttpServletRequestBuilder request(String endpoint) {
        MockHttpServletRequestBuilder r = switch (endpoint) {
            case "GET /me/wallet" -> get("/me/wallet");
            case "GET /me/wallet/transactions" -> get("/me/wallet/transactions");
            case "POST /topups" -> post("/topups").contentType(MediaType.APPLICATION_JSON).content("{\"amountThb\": 500}");
            case "GET /topups/{id}" -> get("/topups/7");
            case "GET /admin/topups" -> get("/admin/topups").param("from", "2026-11-01").param("to", "2026-11-14");
            case "GET /admin/reports/revenue" -> get("/admin/reports/revenue").param("from", "2026-11-01").param("to", "2026-11-14");
            case "GET /admin/wallet-settings" -> get("/admin/wallet-settings");
            case "PUT /admin/wallet-settings" -> put("/admin/wallet-settings").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rate\": 1, \"minThb\": 100, \"maxThb\": 10000, \"qrExpireMinutes\": 15}");
            case "POST /internal/wallets" -> post("/internal/wallets").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\": 12}");
            case "POST /internal/wallets/{id}/debit" -> post("/internal/wallets/4/debit").header("Idempotency-Key", "k")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\": 240, \"type\": \"BOOKING\", \"refType\": \"BOOKING\", \"refId\": 192}");
            case "POST /internal/wallets/{id}/credit" -> post("/internal/wallets/4/credit").header("Idempotency-Key", "k")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\": 120, \"type\": \"REFUND\", \"refType\": \"BOOKING\", \"refId\": 192}");
            default -> throw new IllegalArgumentException("ไม่รู้จัก endpoint: " + endpoint);
        };
        return r.header("X-User-Id", "4");
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder r, String userType, String permissions) {
        if (userType != null) r.header("X-User-Type", userType);
        if (permissions != null) r.header("X-Permissions", permissions);
        return r;
    }

    private void expectForbidden(MockHttpServletRequestBuilder r) throws Exception {
        mockMvc.perform(r)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(walletService, topupService, reportService, paymentPoller);
    }

    private void expectAllowed(MockHttpServletRequestBuilder r) throws Exception {
        mockMvc.perform(r).andExpect(status().is2xxSuccessful());
    }

    // ------------------------------------------------------------------ สมาชิกเท่านั้น

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /me/wallet", "GET /me/wallet/transactions", "POST /topups", "GET /topups/{id}"})
    @DisplayName("TC-SEC-01 endpoint ของสมาชิก: พนักงาน (แม้มีทุกสิทธิ์) และคนที่ไม่มี X-User-Type → 403")
    void memberOnly_rejectsOthers(String endpoint) throws Exception {
        expectForbidden(as(request(endpoint), "STAFF", ALL_PERMS));
        expectForbidden(as(request(endpoint), null, null));
        expectForbidden(as(request(endpoint), "member", null));        // ตัวพิมพ์เล็กไม่นับ
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /me/wallet", "GET /me/wallet/transactions", "POST /topups", "GET /topups/{id}"})
    @DisplayName("TC-SEC-02 endpoint ของสมาชิก: MEMBER → ผ่าน")
    void memberOnly_allowsMember(String endpoint) throws Exception {
        expectAllowed(as(request(endpoint), "MEMBER", null));
    }

    // ------------------------------------------------------------------ report.view

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /admin/topups", "GET /admin/reports/revenue"})
    @DisplayName("TC-SEC-03 รายงาน: MEMBER ที่ปลอม permission / STAFF ไม่มี report.view / ชื่อคล้าย → 403")
    void reportView_rejectsOthers(String endpoint) throws Exception {
        expectForbidden(as(request(endpoint), "MEMBER", "report.view"));
        expectForbidden(as(request(endpoint), "STAFF", null));
        expectForbidden(as(request(endpoint), "STAFF", "setting.manage"));
        expectForbidden(as(request(endpoint), "STAFF", "report.view.all"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /admin/topups", "GET /admin/reports/revenue"})
    @DisplayName("TC-SEC-04 รายงาน: STAFF + report.view (อยู่ในรายการหลายสิทธิ์) → ผ่าน")
    void reportView_allowsPermission(String endpoint) throws Exception {
        expectAllowed(as(request(endpoint), "STAFF", "setting.manage,report.view"));
    }

    // ------------------------------------------------------------------ setting.manage

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /admin/wallet-settings", "PUT /admin/wallet-settings"})
    @DisplayName("TC-SEC-05 ค่าตั้ง: MEMBER ที่ปลอม permission / STAFF ที่มีแค่ report.view → 403")
    void settingManage_rejectsOthers(String endpoint) throws Exception {
        expectForbidden(as(request(endpoint), "MEMBER", "setting.manage"));
        expectForbidden(as(request(endpoint), "STAFF", null));
        expectForbidden(as(request(endpoint), "STAFF", "report.view"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /admin/wallet-settings", "PUT /admin/wallet-settings"})
    @DisplayName("TC-SEC-06 ค่าตั้ง: STAFF + setting.manage → ผ่าน")
    void settingManage_allowsPermission(String endpoint) throws Exception {
        expectAllowed(as(request(endpoint), "STAFF", "setting.manage"));
    }

    // ------------------------------------------------------------------ /internal/**

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST /internal/wallets", "POST /internal/wallets/{id}/debit", "POST /internal/wallets/{id}/credit"})
    @DisplayName("TC-SEC-07 /internal: ไม่มี token / token ผิด / header สมาชิกหรือพนักงานไม่ช่วย → 401")
    void internal_rejectsWithoutToken(String endpoint) throws Exception {
        for (MockHttpServletRequestBuilder r : List.of(
                request(endpoint),
                request(endpoint).header("X-Internal-Token", "wrong"),
                as(request(endpoint), "STAFF", ALL_PERMS),
                as(request(endpoint), "MEMBER", null))) {
            mockMvc.perform(r)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_TOKEN"));
        }
        verifyNoInteractions(walletService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST /internal/wallets", "POST /internal/wallets/{id}/debit", "POST /internal/wallets/{id}/credit"})
    @DisplayName("TC-SEC-08 /internal: token ถูก → ผ่าน (ไม่ต้องมี X-User-Type)")
    void internal_allowsToken(String endpoint) throws Exception {
        expectAllowed(request(endpoint).header("X-Internal-Token", TOKEN));
    }

    // ------------------------------------------------------------------ สาธารณะ

    @Test
    @DisplayName("TC-SEC-09 /topups/config เปิดให้ทุกคน ไม่ต้องมี header")
    void config_isPublic() throws Exception {
        mockMvc.perform(get("/topups/config")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ กันลืมใส่สิทธิ์ใน endpoint ใหม่

    /** endpoint ที่ "ตั้งใจ" ไม่มี annotation สิทธิ์ พร้อมเหตุผล */
    private static final Set<String> NO_ANNOTATION_ALLOWED = Set.of(
            "/topups/config",                       // สาธารณะ: หน้าเติมเงินต้องเห็นก่อน login
            "/webhooks/payment",                    // gateway ภายนอกเรียก กันด้วยลายเซ็น HMAC
            "/internal/wallets",                    // service ภายในเรียก กันด้วย X-Internal-Token (WebConfig)
            "/internal/wallets/{userId}/debit",
            "/internal/wallets/{userId}/credit");

    @Test
    @DisplayName("TC-SEC-10 ทุก endpoint ต้องมี @MemberOnly / @StaffOnly / @RequirePermission ยกเว้นที่อยู่ใน allowlist")
    void everyEndpointIsGuarded() {
        List<String> unguarded = new ArrayList<>();
        Set<String> allPaths = new HashSet<>();

        mapping.getHandlerMethods().forEach((info, method) -> {
            if (!method.getBeanType().getPackageName().startsWith("com.cosync.wallet")) {
                return;                                     // ข้าม controller ของ Spring เอง (เช่น /error)
            }
            Set<String> paths = info.getPatternValues();
            allPaths.addAll(paths);
            boolean guarded = method.hasMethodAnnotation(MemberOnly.class)
                    || method.hasMethodAnnotation(StaffOnly.class)
                    || method.hasMethodAnnotation(RequirePermission.class);
            if (!guarded && !NO_ANNOTATION_ALLOWED.containsAll(paths)) {
                unguarded.add(info + " → " + method.getShortLogMessage());
            }
        });

        assertEquals(List.of(), unguarded,
                "endpoint เหล่านี้ไม่มี annotation สิทธิ์ ใส่ให้ถูก หรือเพิ่มใน NO_ANNOTATION_ALLOWED พร้อมเหตุผล");
        assertTrue(allPaths.containsAll(NO_ANNOTATION_ALLOWED),
                "allowlist มี path ที่ไม่มีอยู่จริงแล้ว ลบออกจาก NO_ANNOTATION_ALLOWED: " + NO_ANNOTATION_ALLOWED);
    }
}
