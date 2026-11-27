package com.cosync.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.entity.enumtype.EquipmentStatus;
import com.cosync.booking.service.EquipmentService;

/** /admin/equipment: ตัวกรอง, 201, 204, resourceId null ได้, key ตาม spec และ 409 ทั้ง 3 แบบ */
@WebMvcTest(AdminEquipmentController.class)
class AdminEquipmentControllerTest {

    private static final EquipmentResponse CABLE = new EquipmentResponse(1L, "EQ-L-001", "สาย HDMI", 1L,
            EquipmentStatus.AVAILABLE, null, new BigDecimal("20.00"), new BigDecimal("300.00"));

    @Autowired MockMvc mockMvc;

    @MockitoBean EquipmentService equipmentService;

    private static MockHttpServletRequestBuilder asIt(MockHttpServletRequestBuilder r) {
        return r.header("X-User-Type", "STAFF").header("X-Permissions", "equipment.manage");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String body) {
        return asIt(r).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("TC-EQC-H01 GET ไม่มีตัวกรอง → null ทั้งคู่ · key ตาม spec (resourceCode เป็น null ได้)")
    void list_noFilter() throws Exception {
        when(equipmentService.listEquipment(isNull(), isNull())).thenReturn(List.of(CABLE));

        mockMvc.perform(asIt(get("/admin/equipment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentId").value(1))
                .andExpect(jsonPath("$[0].barcode").value("EQ-L-001"))
                .andExpect(jsonPath("$[0].typeName").value("สาย HDMI"))
                .andExpect(jsonPath("$[0].equipmentTypeId").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].resourceCode").isEmpty())
                .andExpect(jsonPath("$[0].loanFee").value(20.00));
    }

    @Test
    @DisplayName("TC-EQC-H02 GET ?status=MAINTENANCE&typeId=3 → ส่งต่อให้ service · status พิมพ์ผิด → 400")
    void list_filters() throws Exception {
        when(equipmentService.listEquipment(EquipmentStatus.MAINTENANCE, 3L)).thenReturn(List.of());

        mockMvc.perform(asIt(get("/admin/equipment")).param("status", "MAINTENANCE").param("typeId", "3"))
                .andExpect(status().isOk());
        verify(equipmentService).listEquipment(EquipmentStatus.MAINTENANCE, 3L);

        mockMvc.perform(asIt(get("/admin/equipment")).param("status", "BROKEN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-EQC-H03 POST ของที่ Counter (ไม่มี resourceId) → 201")
    void create_counterItem() throws Exception {
        when(equipmentService.createEquipment(any())).thenReturn(CABLE);

        mockMvc.perform(json(post("/admin/equipment"), "{\"equipmentTypeId\": 1, \"barcode\": \"EQ-L-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(1));
    }

    @Test
    @DisplayName("TC-EQC-H04 POST ไม่มีประเภท / barcode ว่าง / barcode ยาว 31 → 400")
    void create_invalid() throws Exception {
        mockMvc.perform(json(post("/admin/equipment"), "{\"barcode\": \"EQ-L-009\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.equipmentTypeId").exists());
        mockMvc.perform(json(post("/admin/equipment"), "{\"equipmentTypeId\": 1, \"barcode\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.barcode").exists());
        mockMvc.perform(json(post("/admin/equipment"),
                        "{\"equipmentTypeId\": 1, \"barcode\": \"" + "X".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.barcode").exists());
        verifyNoInteractions(equipmentService);
    }

    @Test
    @DisplayName("TC-EQC-H05 barcode ซ้ำ (uk_equipment_barcode) → 409 BARCODE_TAKEN")
    void create_duplicate() throws Exception {
        when(equipmentService.createEquipment(any())).thenThrow(new DataIntegrityViolationException(
                "Duplicate entry 'EQ-L-001' for key 'equipment.uk_equipment_barcode'"));

        mockMvc.perform(json(post("/admin/equipment"), "{\"equipmentTypeId\": 1, \"barcode\": \"EQ-L-001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BARCODE_TAKEN"));
    }

    @Test
    @DisplayName("TC-EQC-H06 PUT /admin/equipment/1 → 200 ส่ง id จาก path · ถูกยืมอยู่ → 409 EQUIPMENT_IN_USE")
    void update() throws Exception {
        when(equipmentService.updateEquipment(eq(1L), any())).thenReturn(CABLE);
        mockMvc.perform(json(put("/admin/equipment/1"),
                        "{\"equipmentTypeId\": 1, \"barcode\": \"EQ-L-001\", \"status\": \"AVAILABLE\"}"))
                .andExpect(status().isOk());

        when(equipmentService.updateEquipment(eq(2L), any())).thenThrow(
                new ApiException(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE", "ถูกยืมอยู่"));
        mockMvc.perform(json(put("/admin/equipment/2"), "{\"equipmentTypeId\": 1, \"barcode\": \"EQ-L-002\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_IN_USE"));
    }

    @Test
    @DisplayName("TC-EQC-H07 DELETE → 204 ไม่มี body")
    void delete_ok() throws Exception {
        mockMvc.perform(asIt(delete("/admin/equipment/1")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(equipmentService).deleteEquipment(1L);
    }

    @Test
    @DisplayName("TC-EQC-H08 DELETE ของที่มีประวัติ → 409 EQUIPMENT_HAS_HISTORY พร้อม body error")
    void delete_hasHistory() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "EQUIPMENT_HAS_HISTORY", "มีประวัติการยืม"))
                .when(equipmentService).deleteEquipment(5L);

        mockMvc.perform(asIt(delete("/admin/equipment/5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_HAS_HISTORY"));
    }

    @Test
    @DisplayName("TC-EQC-H09 ผู้จัดการ (resource.manage) / สมาชิก จัดการอุปกรณ์ไม่ได้ → 403")
    void forbidden() throws Exception {
        mockMvc.perform(delete("/admin/equipment/1")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "resource.manage"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/equipment").header("X-User-Type", "MEMBER"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(equipmentService);
    }
}
