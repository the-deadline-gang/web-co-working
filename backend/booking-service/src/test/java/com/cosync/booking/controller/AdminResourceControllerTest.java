package com.cosync.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cosync.booking.dto.resource.ResourceResponse;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.service.ResourceService;

/** /admin/resources: ตัวกรองไม่บังคับ, 201, @Valid, ชื่อ key ตาม spec และ code ซ้ำ → 409 */
@WebMvcTest(AdminResourceController.class)
class AdminResourceControllerTest {

    private static final ResourceResponse ROOM = new ResourceResponse("B-ROOM-01", 2L, ServiceType.MEETING, 4,
            null, new BigDecimal("12.00"), null, true, 6L, "B", List.of());

    private static final String MEETING_BODY = """
            {"code": "B-ROOM-03", "zoneId": 2, "serviceType": "MEETING", "capacity": 4, "sizeSqm": 12}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean ResourceService resourceService;

    private static MockHttpServletRequestBuilder asManager(MockHttpServletRequestBuilder r) {
        return r.header("X-User-Type", "STAFF").header("X-Permissions", "resource.manage");
    }

    private static MockHttpServletRequestBuilder postJson(String body) {
        return asManager(post("/admin/resources")).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("TC-RSC-H01 GET ไม่มีตัวกรอง → ส่ง null ทั้งคู่ · key ตาม spec (resourceId, zoneName, isActive, fixedEquipment)")
    void list_noFilter() throws Exception {
        when(resourceService.listResource(isNull(), isNull())).thenReturn(List.of(ROOM));

        mockMvc.perform(asManager(get("/admin/resources")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceId").value(6))
                .andExpect(jsonPath("$[0].code").value("B-ROOM-01"))
                .andExpect(jsonPath("$[0].zoneId").value(2))
                .andExpect(jsonPath("$[0].zoneName").value("B"))
                .andExpect(jsonPath("$[0].serviceType").value("MEETING"))
                .andExpect(jsonPath("$[0].dailyQuota").doesNotExist())
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[0].fixedEquipment").isArray());
    }

    @Test
    @DisplayName("TC-RSC-H02 GET ?serviceType=DESK&zoneId=1 → ส่งค่าตัวกรองไปที่ service")
    void list_withFilters() throws Exception {
        when(resourceService.listResource(ServiceType.DESK, 1L)).thenReturn(List.of());

        mockMvc.perform(asManager(get("/admin/resources")).param("serviceType", "DESK").param("zoneId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verify(resourceService).listResource(ServiceType.DESK, 1L);
    }

    @Test
    @DisplayName("TC-RSC-H03 GET ?serviceType=MEETNG (พิมพ์ผิด) → 400")
    void list_badEnum() throws Exception {
        mockMvc.perform(asManager(get("/admin/resources")).param("serviceType", "MEETNG"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resourceService);
    }

    @Test
    @DisplayName("TC-RSC-H04 POST ห้องประชุม (ไม่มี dailyQuota) → 201")
    void create_ok() throws Exception {
        when(resourceService.creatResource(any())).thenReturn(ROOM);

        mockMvc.perform(postJson(MEETING_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId").value(6));
    }

    @Test
    @DisplayName("TC-RSC-H05 POST ว่าง → 400 บอกครบ 4 ช่องที่บังคับ")
    void create_empty() throws Exception {
        mockMvc.perform(postJson("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("กรุณากรอกรหัส resource"))
                .andExpect(jsonPath("$.details.zoneId").value("กรุณาเลือกโซน"))
                .andExpect(jsonPath("$.details.serviceType").value("กรุณาเลือกประเภทบริการ"))
                .andExpect(jsonPath("$.details.capacity").value("กรุณากรอกจำนวนคนที่รองรับได้"));
        verifyNoInteractions(resourceService);
    }

    @Test
    @DisplayName("TC-RSC-H06 capacity = 0 / dailyQuota = 0 / code ยาว 21 ตัว / sizeSqm = 0 → 400")
    void create_badValues() throws Exception {
        mockMvc.perform(postJson(MEETING_BODY.replace("\"capacity\": 4", "\"capacity\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.capacity").value("ต้องรองรับได้อย่างน้อย 1 คน"));
        mockMvc.perform(postJson("""
                {"code": "A-DP", "zoneId": 1, "serviceType": "DAY_PASS", "capacity": 40, "dailyQuota": 0}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.dailyQuota").exists());
        mockMvc.perform(postJson(MEETING_BODY.replace("B-ROOM-03", "X".repeat(21))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").exists());
        mockMvc.perform(postJson(MEETING_BODY.replace("\"sizeSqm\": 12", "\"sizeSqm\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sizeSqm").exists());
        verifyNoInteractions(resourceService);
    }

    @Test
    @DisplayName("TC-RSC-H07 code ซ้ำ (uk_resource_code) → 409 RESOURCE_CODE_TAKEN")
    void create_duplicateCode() throws Exception {
        when(resourceService.creatResource(any())).thenThrow(new DataIntegrityViolationException(
                "Duplicate entry 'B-ROOM-01' for key 'resource.uk_resource_code'"));

        mockMvc.perform(postJson(MEETING_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CODE_TAKEN"));
    }

    @Test
    @DisplayName("TC-RSC-H08 PUT /admin/resources/6 → 200 ส่ง resourceId จาก path")
    void update_ok() throws Exception {
        when(resourceService.update(eq(6L), any())).thenReturn(ROOM);

        mockMvc.perform(asManager(put("/admin/resources/6"))
                        .contentType(MediaType.APPLICATION_JSON).content(MEETING_BODY))
                .andExpect(status().isOk());
        verify(resourceService).update(eq(6L), any());
    }

    @Test
    @DisplayName("TC-RSC-H09 สมาชิก / พนักงานที่มีแค่ equipment.manage → 403")
    void forbidden() throws Exception {
        mockMvc.perform(get("/admin/resources").header("X-User-Type", "MEMBER"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/resources")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "equipment.manage")
                        .contentType(MediaType.APPLICATION_JSON).content(MEETING_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(resourceService);
    }
}
