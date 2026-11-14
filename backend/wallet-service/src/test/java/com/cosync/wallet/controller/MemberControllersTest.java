package com.cosync.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.service.PaymentPoller;
import com.cosync.wallet.service.TopupService;
import com.cosync.wallet.service.WalletService;

/** controller ฝั่งสมาชิก: /me/wallet และ /topups */
@WebMvcTest({WalletController.class, TopupController.class})
@TestPropertySource(properties = "app.internal.token=test-internal-token")
class MemberControllersTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WalletService walletService;
    @MockitoBean TopupService topupService;
    @MockitoBean PaymentPoller paymentPoller;

    @Test
    @DisplayName("TC-MEM-01 สมาชิกดูยอด → 200 ใช้ X-User-Id จาก gateway")
    void myWallet() throws Exception {
        when(walletService.getByUserId(4L)).thenReturn(new WalletResponse(1L, "W-000001", new BigDecimal("360.00")));

        mockMvc.perform(get("/me/wallet").header("X-User-Id", "4").header("X-User-Type", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(360.0));
    }

    @Test
    @DisplayName("TC-MEM-02 พนักงานเรียก /me/wallet → 403")
    void myWallet_staff() throws Exception {
        mockMvc.perform(get("/me/wallet").header("X-User-Id", "1").header("X-User-Type", "STAFF"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-MEM-03 ประวัติ กรอง type และค่าเริ่มต้น page 0 size 20")
    void transactions() throws Exception {
        when(walletService.getTransactions(4L, TransactionType.TOPUP, 0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/me/wallet/transactions").param("type", "TOPUP")
                        .header("X-User-Id", "4").header("X-User-Type", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("TC-MEM-04 type ไม่รู้จัก → 400")
    void transactions_badType() throws Exception {
        mockMvc.perform(get("/me/wallet/transactions").param("type", "GIFT")
                        .header("X-User-Id", "4").header("X-User-Type", "MEMBER"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-MEM-05 config เปิดให้ทุกคนดูได้")
    void config_public() throws Exception {
        when(topupService.getConfig()).thenReturn(
                new TopupConfigResponse(new BigDecimal("1.0000"), 100, 10000, List.of(100, 200), 15));

        mockMvc.perform(get("/topups/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quickAmounts[1]").value(200));
    }

    @Test
    @DisplayName("TC-MEM-06 สร้าง topup → 201")
    void createTopup() throws Exception {
        when(topupService.create(eq(4L), any())).thenReturn(new TopupResponse(7L, "PAY-20261114-0007", 500,
                new BigDecimal("500.00"), TopupStatus.PENDING, "data:image/png;base64,QQ",
                Instant.parse("2026-11-14T05:15:00Z"), null, null, null));

        mockMvc.perform(post("/topups").header("X-User-Id", "4").header("X-User-Type", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amountThb\": 500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("TC-MEM-07 ไม่ส่ง amountThb → 400")
    void createTopup_missingAmount() throws Exception {
        mockMvc.perform(post("/topups").header("X-User-Id", "4").header("X-User-Type", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amountThb").exists());
        verifyNoInteractions(topupService);
    }

    @Test
    @DisplayName("TC-MEM-08 ดูสถานะ topup: ถาม gateway ก่อน แล้วค่อยอ่านสถานะ (userId/topupId ถูกตำแหน่ง)")
    void getTopup() throws Exception {
        when(topupService.get(4L, 7L)).thenReturn(new TopupResponse(7L, "PAY-20261114-0007", 500,
                new BigDecimal("500.00"), TopupStatus.EXPIRED, null, Instant.parse("2026-11-14T05:15:00Z"),
                null, null, null));

        mockMvc.perform(get("/topups/7").header("X-User-Id", "4").header("X-User-Type", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        InOrder order = inOrder(paymentPoller, topupService);
        order.verify(paymentPoller).pollFor(4L, 7L);
        order.verify(topupService).get(4L, 7L);
    }

    @Test
    @DisplayName("TC-MEM-09 พนักงานดู topup → 403 ไม่ถาม gateway")
    void getTopup_staff() throws Exception {
        mockMvc.perform(get("/topups/7").header("X-User-Id", "1").header("X-User-Type", "STAFF"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(paymentPoller, topupService);
    }
}
