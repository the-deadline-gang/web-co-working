package com.cosync.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.wallet.dto.setting.WalletSettingsResponse;
import com.cosync.wallet.service.WalletService;

@WebMvcTest(AdminWalletSettingController.class)
@TestPropertySource(properties = "app.internal.token=test-internal-token")
class AdminWalletSettingControllerTest {

    private static final WalletSettingsResponse SETTINGS = new WalletSettingsResponse(new BigDecimal("1.0000"),
            100, 10000, 15, "STF-004", Instant.parse("2026-11-14T05:00:00Z"));

    @Autowired MockMvc mockMvc;

    @MockitoBean WalletService walletService;

    @Test
    @DisplayName("TC-SET-H01 setting.manage ดูค่าตั้ง → 200")
    void get_ok() throws Exception {
        when(walletService.getSettings()).thenReturn(SETTINGS);

        mockMvc.perform(get("/admin/wallet-settings")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("STF-004"));
    }

    @Test
    @DisplayName("TC-SET-H02 ไม่มีสิทธิ์ → 403")
    void get_forbidden() throws Exception {
        mockMvc.perform(get("/admin/wallet-settings")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("TC-SET-H03 แก้ค่าตั้ง → ส่ง staffId จาก X-User-Id")
    void update_ok() throws Exception {
        when(walletService.updateSettings(eq(4L), any())).thenReturn(SETTINGS);

        mockMvc.perform(put("/admin/wallet-settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\": 1, \"minThb\": 100, \"maxThb\": 10000, \"qrExpireMinutes\": 15}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-SET-H04 qrExpireMinutes เกิน 60 → 400")
    void update_invalid() throws Exception {
        mockMvc.perform(put("/admin/wallet-settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\": 1, \"minThb\": 100, \"maxThb\": 10000, \"qrExpireMinutes\": 61}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.qrExpireMinutes").exists());
        verifyNoInteractions(walletService);
    }
}
