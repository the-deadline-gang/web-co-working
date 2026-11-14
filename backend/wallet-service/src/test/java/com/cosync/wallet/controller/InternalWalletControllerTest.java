package com.cosync.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.service.WalletService;

@WebMvcTest(InternalWalletController.class)
@TestPropertySource(properties = "app.internal.token=test-internal-token")
class InternalWalletControllerTest {

    private static final String TOKEN = "test-internal-token";
    private static final String DEBIT_BODY = """
            {"amount": 240, "type": "BOOKING", "refType": "BOOKING", "refId": 192, "note": "B-ROOM-02"}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean WalletService walletService;

    @Test
    @DisplayName("TC-INT-H01 สร้าง wallet ด้วย token ถูก → 200")
    void create_ok() throws Exception {
        when(walletService.createIfAbsent(12L)).thenReturn(new WalletResponse(7L, "W-000007", new BigDecimal("0.00")));

        mockMvc.perform(post("/internal/wallets").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": 12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayId").value("W-000007"));
    }

    @Test
    @DisplayName("TC-INT-H02 ไม่มี / ผิด token → 401 ไม่ถึง service")
    void create_noToken() throws Exception {
        mockMvc.perform(post("/internal/wallets")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": 12}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_TOKEN"));
        mockMvc.perform(post("/internal/wallets").header("X-Internal-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\": 12}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-INT-H03 ไม่ส่ง userId → 400")
    void create_missingUserId() throws Exception {
        mockMvc.perform(post("/internal/wallets").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-INT-H04 debit สำเร็จ → 200 พร้อมยอดหลังตัด")
    void debit_ok() throws Exception {
        when(walletService.debit(eq(4L), eq("booking-192-pay"), any())).thenReturn(new TransactionResponse(
                10L, TransactionType.BOOKING, new BigDecimal("-240.00"), new BigDecimal("60.00"),
                RefType.BOOKING, 192L, "B-ROOM-02", Instant.parse("2026-11-14T05:00:00Z")));

        mockMvc.perform(post("/internal/wallets/4/debit").header("X-Internal-Token", TOKEN)
                        .header("Idempotency-Key", "booking-192-pay")
                        .contentType(MediaType.APPLICATION_JSON).content(DEBIT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnId").value(10))
                .andExpect(jsonPath("$.balanceAfter").value(60.0));
    }

    @Test
    @DisplayName("TC-INT-H05 ไม่ส่ง Idempotency-Key → 400")
    void debit_missingKey() throws Exception {
        mockMvc.perform(post("/internal/wallets/4/debit").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(DEBIT_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['Idempotency-Key']").exists());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-INT-H06 amount ติดลบ → 400 ที่ field amount")
    void debit_negativeAmount() throws Exception {
        mockMvc.perform(post("/internal/wallets/4/debit").header("X-Internal-Token", TOKEN)
                        .header("Idempotency-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -1, \"type\": \"BOOKING\", \"refType\": \"BOOKING\", \"refId\": 192}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-INT-H07 ยอดไม่พอ → 422 พร้อม details")
    void debit_insufficient() throws Exception {
        when(walletService.debit(eq(4L), eq("k"), any())).thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                "INSUFFICIENT_BALANCE", "ยอด Wallet ไม่พอ", Map.of("balance", "60.00", "shortfall", "180.00")));

        mockMvc.perform(post("/internal/wallets/4/debit").header("X-Internal-Token", TOKEN)
                        .header("Idempotency-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(DEBIT_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.details.shortfall").value("180.00"));
    }

    @Test
    @DisplayName("TC-INT-H08 credit ส่งต่อไป service")
    void credit_ok() throws Exception {
        when(walletService.credit(eq(4L), eq("refund-1"), any())).thenReturn(new TransactionResponse(
                11L, TransactionType.REFUND, new BigDecimal("120.00"), new BigDecimal("180.00"),
                RefType.BOOKING, 192L, null, Instant.parse("2026-11-14T05:00:00Z")));

        mockMvc.perform(post("/internal/wallets/4/credit").header("X-Internal-Token", TOKEN)
                        .header("Idempotency-Key", "refund-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 120, \"type\": \"REFUND\", \"refType\": \"BOOKING\", \"refId\": 192}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REFUND"));
    }
}
