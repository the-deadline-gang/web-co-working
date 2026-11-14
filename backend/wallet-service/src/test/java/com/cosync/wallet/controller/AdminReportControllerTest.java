package com.cosync.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.report.RevenueReport;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.service.ReportService;
import com.cosync.wallet.service.TopupService;

@WebMvcTest(AdminReportController.class)
@TestPropertySource(properties = "app.internal.token=test-internal-token")
class AdminReportControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 14);

    @Autowired MockMvc mockMvc;

    @MockitoBean TopupService topupService;
    @MockitoBean ReportService reportService;

    @Test
    @DisplayName("TC-ADM-H01 Finance (report.view) ดูรายงานรายได้ → 200")
    void revenue_ok() throws Exception {
        BigDecimal z = new BigDecimal("0.00");
        when(reportService.revenue(TO, TO)).thenReturn(new RevenueReport(TO, TO,
                List.of(new RevenueReport.Day(TO, new BigDecimal("1500.00"), new BigDecimal("240.00"), z, z, z))));

        mockMvc.perform(get("/admin/reports/revenue").param("from", "2026-11-14").param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].date").value("2026-11-14"))
                .andExpect(jsonPath("$.days[0].topup").value(1500.0));
    }

    @Test
    @DisplayName("TC-ADM-H02 STAFF ไม่มีสิทธิ์ / MEMBER → 403 ไม่ถึง service")
    void revenue_forbidden() throws Exception {
        mockMvc.perform(get("/admin/reports/revenue").param("from", "2026-11-01").param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "setting.manage"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports/revenue").param("from", "2026-11-01").param("to", "2026-11-14")
                        .header("X-User-Type", "MEMBER").header("X-Permissions", "report.view"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("TC-ADM-H03 ไม่ส่ง from / วันที่ผิดรูปแบบ → 400")
    void revenue_badParams() throws Exception {
        mockMvc.perform(get("/admin/reports/revenue").param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.from").exists());
        mockMvc.perform(get("/admin/reports/revenue").param("from", "14/11/2026").param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("TC-ADM-H04 ดูรายการเติมเงิน กรองสถานะ ส่งค่าไป service ถูกต้อง")
    void topups_ok() throws Exception {
        when(topupService.searchForAdmin(eq(TopupStatus.PAID), eq(FROM), eq(TO), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 1, 50, 0));

        mockMvc.perform(get("/admin/topups").param("status", "PAID").param("from", "2026-11-01")
                        .param("to", "2026-11-14").param("page", "1").param("size", "50")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));

        verify(topupService).searchForAdmin(TopupStatus.PAID, FROM, TO, 1, 50);
    }

    @Test
    @DisplayName("TC-ADM-H05 status ไม่รู้จัก → 400")
    void topups_badStatus() throws Exception {
        mockMvc.perform(get("/admin/topups").param("status", "DONE").param("from", "2026-11-01")
                        .param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(topupService);
    }

    @Test
    @DisplayName("TC-ADM-H06 ไม่ส่ง status → ส่ง null (ทุกสถานะ) page/size ค่าเริ่มต้น 0/20")
    void topups_defaults() throws Exception {
        when(topupService.searchForAdmin(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/admin/topups").param("from", "2026-11-01").param("to", "2026-11-14")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "report.view"))
                .andExpect(status().isOk());

        verify(topupService).searchForAdmin(null, FROM, TO, 0, 20);
    }
}
