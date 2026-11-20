package com.cosync.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.booking.dto.setting.BookingSettingsResponse;
import com.cosync.booking.service.SettingService;

/** ชั้น HTTP: path, สิทธิ์, @Valid, X-User-Id และรูป JSON ที่ตอบกลับ (กฎธุรกิจอยู่ใน SettingServiceTest) */
@WebMvcTest(AdminSettingController.class)
class AdminSettingControllerTest {

    private static final BookingSettingsResponse SETTINGS = new BookingSettingsResponse(
            LocalTime.of(8, 0), LocalTime.of(22, 0), 30, 10, 15, 15, 30, 24, 2, 3, 3,
            "STF-004", Instant.parse("2026-11-20T06:00:00Z"));

    /** ค่าตาม seed V2 */
    private static final String VALID_BODY = """
            {"openTime": "08:00", "closeTime": "22:00", "bookAheadDays": 30,
             "holdMinutes": 10, "holdExtendMinutes": 15, "earlyCheckinMinutes": 15,
             "noShowMinutes": 30, "cancelFullRefundHours": 24, "cancelHalfRefundHours": 2,
             "loanMaxItems": 3, "lostAfterDays": 3}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean SettingService settingService;

    @Test
    @DisplayName("TC-BST-H01 setting.manage ดูค่าตั้ง → 200 ครบ 11 ค่า เวลาเป็น HH:mm:ss")
    void get_ok() throws Exception {
        when(settingService.getSettings()).thenReturn(SETTINGS);

        mockMvc.perform(get("/admin/settings")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openTime").value("08:00:00"))
                .andExpect(jsonPath("$.closeTime").value("22:00:00"))
                .andExpect(jsonPath("$.noShowMinutes").value(30))
                .andExpect(jsonPath("$.cancelFullRefundHours").value(24))
                .andExpect(jsonPath("$.cancelHalfRefundHours").value(2))
                .andExpect(jsonPath("$.lostAfterDays").value(3))
                .andExpect(jsonPath("$.updatedBy").value("STF-004"));
    }

    @Test
    @DisplayName("TC-BST-H02 พนักงานไม่มีสิทธิ์ setting.manage → 403 ไม่เรียก service")
    void get_wrongPermission() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "rate.manage"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(settingService);
    }

    @Test
    @DisplayName("TC-BST-H03 สมาชิกเรียก → 403")
    void get_member() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .header("X-User-Type", "MEMBER").header("X-Permissions", "setting.manage"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(settingService);
    }

    @Test
    @DisplayName("TC-BST-H04 PUT ค่าถูก → 200 ส่ง staffId จาก X-User-Id ไปที่ service")
    void put_ok() throws Exception {
        when(settingService.updateSettings(eq(4L), any())).thenReturn(SETTINGS);

        mockMvc.perform(put("/admin/settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("STF-004"));
    }

    @Test
    @DisplayName("TC-BST-H05 PUT ไม่มีสิทธิ์ → 403 ไม่แก้อะไร")
    void put_forbidden() throws Exception {
        mockMvc.perform(put("/admin/settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(settingService);
    }

    @Test
    @DisplayName("TC-BST-H06 ขาด holdMinutes → 400 บอกช่องที่ผิด")
    void put_missingField() throws Exception {
        String body = VALID_BODY.replace("\"holdMinutes\": 10, ", "");

        mockMvc.perform(put("/admin/settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.holdMinutes").value("กรุณากรอกเวลาล็อกที่"));
        verifyNoInteractions(settingService);
    }

    @Test
    @DisplayName("TC-BST-H07 holdMinutes = 61 → 400 ข้อความแสดงเลขสูงสุดจริง")
    void put_holdTooLong() throws Exception {
        String body = VALID_BODY.replace("\"holdMinutes\": 10", "\"holdMinutes\": 61");

        mockMvc.perform(put("/admin/settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.holdMinutes").value("เวลาล็อกที่ต้องไม่เกิน 60 นาที"));
        verifyNoInteractions(settingService);
    }

    @Test
    @DisplayName("TC-BST-H08 POST (method ผิด) → 405 ตาม spec ต้องเป็น PUT")
    void post_notAllowed() throws Exception {
        mockMvc.perform(post("/admin/settings").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(settingService);
    }
}
