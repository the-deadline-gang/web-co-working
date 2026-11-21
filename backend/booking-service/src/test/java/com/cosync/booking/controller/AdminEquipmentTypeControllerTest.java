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

import com.cosync.booking.dto.equipment.EquipmentTypeResponse;
import com.cosync.booking.service.EquipmentTypeService;

/** /admin/equipment-types: IT (equipment.manage) เพิ่มประเภท ฝ่ายการเงิน (rate.manage) ตั้งราคา */
@WebMvcTest(AdminEquipmentTypeController.class)
class AdminEquipmentTypeControllerTest {

    private static final EquipmentTypeResponse HDMI = new EquipmentTypeResponse(
            1L, "สาย HDMI", true, new BigDecimal("20.00"), new BigDecimal("300.00"));
    private static final EquipmentTypeResponse NEW_MIC = new EquipmentTypeResponse(
            6L, "ไมค์ตั้งโต๊ะ", true, null, null);

    @Autowired MockMvc mockMvc;

    @MockitoBean EquipmentTypeService equipmentTypeService;

    private static MockHttpServletRequestBuilder asIt(MockHttpServletRequestBuilder r) {
        return r.header("X-User-Id", "7").header("X-User-Type", "STAFF").header("X-Permissions", "equipment.manage");
    }

    private static MockHttpServletRequestBuilder asFinance(MockHttpServletRequestBuilder r) {
        return r.header("X-User-Id", "4").header("X-User-Type", "STAFF").header("X-Permissions", "rate.manage");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String body) {
        return r.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("TC-EQT-H01 GET → 200 key ตาม spec: equipmentTypeId, isLoanable, loanFee (F ใหญ่), deposit")
    void list_ok() throws Exception {
        when(equipmentTypeService.listEquipmentType()).thenReturn(List.of(HDMI));

        mockMvc.perform(asIt(get("/admin/equipment-types")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentTypeId").value(1))
                .andExpect(jsonPath("$[0].name").value("สาย HDMI"))
                .andExpect(jsonPath("$[0].isLoanable").value(true))
                .andExpect(jsonPath("$[0].loanFee").value(20.00))
                .andExpect(jsonPath("$[0].deposit").value(300.00));
    }

    @Test
    @DisplayName("TC-EQT-H02 POST → 201 ราคาเป็น null (ยังไม่ได้ตั้ง)")
    void create_ok() throws Exception {
        when(equipmentTypeService.createEquipmentType(any())).thenReturn(NEW_MIC);

        mockMvc.perform(json(asIt(post("/admin/equipment-types")), "{\"name\": \"ไมค์ตั้งโต๊ะ\", \"isLoanable\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentTypeId").value(6))
                .andExpect(jsonPath("$.loanFee").doesNotExist())
                .andExpect(jsonPath("$.deposit").doesNotExist());
    }

    @Test
    @DisplayName("TC-EQT-H03 POST ไม่ส่ง isLoanable → 400 (ถ้าเป็น boolean ตัวเล็กจะได้ false เงียบๆ)")
    void create_missingLoanable() throws Exception {
        mockMvc.perform(json(asIt(post("/admin/equipment-types")), "{\"name\": \"สาย HDMI 2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.isLoanable").exists());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @DisplayName("TC-EQT-H04 POST ชื่อว่าง / ยาวเกิน 100 → 400")
    void create_badName() throws Exception {
        mockMvc.perform(json(asIt(post("/admin/equipment-types")), "{\"name\": \"  \", \"isLoanable\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
        mockMvc.perform(json(asIt(post("/admin/equipment-types")),
                        "{\"name\": \"" + "x".repeat(101) + "\", \"isLoanable\": true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @DisplayName("TC-EQT-H05 ฝ่ายการเงิน (rate.manage) เพิ่มประเภทเองไม่ได้ / ดูรายการไม่ได้ → 403")
    void finance_cannotManageTypes() throws Exception {
        mockMvc.perform(json(asFinance(post("/admin/equipment-types")), "{\"name\": \"X\", \"isLoanable\": true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(asFinance(get("/admin/equipment-types")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @DisplayName("TC-EQT-H06 PUT pricing โดยฝ่ายการเงิน → 200 ส่ง staffId และ id จาก path")
    void pricing_ok() throws Exception {
        when(equipmentTypeService.setPricing(eq(4L), eq(1L), any())).thenReturn(HDMI);

        mockMvc.perform(json(asFinance(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": 20, \"deposit\": 300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanFee").value(20.00));
    }

    @Test
    @DisplayName("TC-EQT-H07 IT (equipment.manage) ตั้งราคาเองไม่ได้ → 403")
    void it_cannotSetPricing() throws Exception {
        mockMvc.perform(json(asIt(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": 0, \"deposit\": 0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @DisplayName("TC-EQT-H08 PUT pricing ค่ายืม 0 ได้ (ยืมฟรี) แต่ติดลบ / ทศนิยม 3 ตำแหน่ง / ไม่ส่ง → 400")
    void pricing_validation() throws Exception {
        when(equipmentTypeService.setPricing(eq(4L), eq(1L), any())).thenReturn(HDMI);
        mockMvc.perform(json(asFinance(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": 0, \"deposit\": 300}"))
                .andExpect(status().isOk());

        mockMvc.perform(json(asFinance(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": -1, \"deposit\": 300}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.loanFee").exists());
        mockMvc.perform(json(asFinance(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": 20, \"deposit\": 300.555}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.deposit").exists());
        mockMvc.perform(json(asFinance(put("/admin/equipment-types/1/pricing")), "{\"loanFee\": 20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.deposit").exists());
    }

    @Test
    @DisplayName("TC-EQT-H09 id ใน path ไม่ใช่ตัวเลข → 400")
    void pricing_badId() throws Exception {
        mockMvc.perform(json(asFinance(put("/admin/equipment-types/abc/pricing")), "{\"loanFee\": 20, \"deposit\": 300}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(equipmentTypeService);
    }
}
