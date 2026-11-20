package com.cosync.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cosync.booking.dto.rate.RateResponse;
import com.cosync.booking.entity.enumtype.RateUnit;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.service.RateService;

/** ชั้น HTTP ของ /admin/rates: สิทธิ์, enum ใน path, @Valid และชื่อ key ใน JSON ตาม spec */
@WebMvcTest(AdminRateController.class)
class AdminRateControllerTest {

    private static final RateResponse MEETING = new RateResponse(
            ServiceType.MEETING, "ห้องประชุม", new BigDecimal("120.00"), RateUnit.HOUR, new BigDecimal("0.00"));

    @Autowired MockMvc mockMvc;

    @MockitoBean RateService rateService;

    private static MockHttpServletRequestBuilder putMeeting(String body) {
        return put("/admin/rates/MEETING").header("X-User-Id", "4")
                .header("X-User-Type", "STAFF").header("X-Permissions", "rate.manage")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("TC-RTE-H01 GET → 200 ชื่อ key ตรง spec (serviceType, unit, discountPct)")
    void list_ok() throws Exception {
        when(rateService.list()).thenReturn(List.of(MEETING));

        mockMvc.perform(get("/admin/rates")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "rate.manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceType").value("MEETING"))
                .andExpect(jsonPath("$[0].name").value("ห้องประชุม"))
                .andExpect(jsonPath("$[0].rate").value(120.00))
                .andExpect(jsonPath("$[0].unit").value("HOUR"))
                .andExpect(jsonPath("$[0].discountPct").value(0.00));
    }

    @Test
    @DisplayName("TC-RTE-H02 GET ด้วยสิทธิ์อื่น / เป็นสมาชิก → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(get("/admin/rates")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/rates")
                        .header("X-User-Type", "MEMBER").header("X-Permissions", "rate.manage"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H03 PUT ถูก → 200 ส่ง staffId และ enum จาก path ไปที่ service")
    void update_ok() throws Exception {
        when(rateService.update(eq(4L), eq(ServiceType.MEETING), any())).thenReturn(MEETING);

        mockMvc.perform(putMeeting("{\"rate\": 120, \"discountPct\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceType").value("MEETING"));
    }

    @Test
    @DisplayName("TC-RTE-H04 สมาชิกพยายามแก้ราคา → 403 ไม่ถึง service")
    void update_member() throws Exception {
        mockMvc.perform(put("/admin/rates/MEETING").header("X-User-Id", "12")
                        .header("X-User-Type", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rate\": 0.01, \"discountPct\": 0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H05 serviceType ใน path พิมพ์ผิด → 400 ไม่ถึง service")
    void update_badEnum() throws Exception {
        mockMvc.perform(put("/admin/rates/DESKK").header("X-User-Id", "4")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "rate.manage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rate\": 40, \"discountPct\": 0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H06 rate = 0 → 400 ข้อความไทย")
    void update_zeroRate() throws Exception {
        mockMvc.perform(putMeeting("{\"rate\": 0, \"discountPct\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.rate").value("ราคาต้องมากกว่า 0"));
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H07 rate ทศนิยม 3 ตำแหน่ง → 400 (ไม่ปล่อยให้ MySQL ปัดเงียบๆ)")
    void update_threeDecimals() throws Exception {
        mockMvc.perform(putMeeting("{\"rate\": 120.555, \"discountPct\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.rate").value("ราคามีทศนิยมได้ไม่เกิน 2 ตำแหน่ง"));
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H08 discountPct = 100.01 → 400")
    void update_discountOver100() throws Exception {
        mockMvc.perform(putMeeting("{\"rate\": 120, \"discountPct\": 100.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.discountPct").value("ส่วนลดต้องไม่เกิน 100%"));
        verifyNoInteractions(rateService);
    }

    @Test
    @DisplayName("TC-RTE-H09 ไม่ส่ง discountPct → 400 บอกให้ใส่ 0")
    void update_missingDiscount() throws Exception {
        mockMvc.perform(putMeeting("{\"rate\": 120}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.discountPct").value("กรุณากรอกส่วนลด (ไม่มีส่วนลดให้ใส่ 0)"));
        verifyNoInteractions(rateService);
    }
}
