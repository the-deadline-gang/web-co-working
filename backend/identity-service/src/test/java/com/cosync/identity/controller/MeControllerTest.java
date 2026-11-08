package com.cosync.identity.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.service.AuthService;

@WebMvcTest(MeController.class)
class MeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;

    @Test
    @DisplayName("TC-ME-01 (HTTP) สมาชิกเรียก /me → 200 และไม่มีข้อมูลลับ")
    void member_ok() throws Exception {
        when(authService.getMe(4L)).thenReturn(
                new MeResponse(4L, "U-000004", "ทดสอบ", "ระบบ", "verify3@example.com", "0800000000"));

        mockMvc.perform(get("/me").header("X-User-Id", "4").header("X-User-Type", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayId").value("U-000004"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.qrSecret").doesNotExist());
    }

    @Test
    @DisplayName("TC-ME-02 (HTTP) พนักงานเรียก /me → 403 ไม่ถึง service")
    void staff_forbidden() throws Exception {
        mockMvc.perform(get("/me").header("X-User-Id", "1").header("X-User-Type", "STAFF"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }
}
