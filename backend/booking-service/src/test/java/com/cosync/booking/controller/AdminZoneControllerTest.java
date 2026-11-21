package com.cosync.booking.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cosync.booking.dto.zone.ZoneResponse;
import com.cosync.booking.service.ZoneService;

/** ชั้น HTTP ของ /admin/zones: path ตาม spec, 201, สิทธิ์, @Valid และชื่อซ้ำ → 409 */
@WebMvcTest(AdminZoneController.class)
class AdminZoneControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ZoneService zoneService;

    private static MockHttpServletRequestBuilder postZone(String body) {
        return post("/admin/zones")
                .header("X-User-Type", "STAFF").header("X-Permissions", "resource.manage")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("TC-ZON-H01 GET /admin/zones → 200 key ชื่อ zoneId ตาม spec")
    void list_ok() throws Exception {
        when(zoneService.listZones()).thenReturn(List.of(new ZoneResponse(1L, "A"), new ZoneResponse(2L, "B")));

        mockMvc.perform(get("/admin/zones")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "resource.manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[1].name").value("B"));
    }

    @Test
    @DisplayName("TC-ZON-H02 ไม่มี resource.manage / เป็นสมาชิก → 403")
    void forbidden() throws Exception {
        mockMvc.perform(get("/admin/zones")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "equipment.manage"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/zones").header("X-User-Type", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"D\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(zoneService);
    }

    @Test
    @DisplayName("TC-ZON-H03 POST /admin/zones → 201 พร้อม zoneId ใหม่")
    void create_ok() throws Exception {
        when(zoneService.createZone(any())).thenReturn(new ZoneResponse(4L, "D"));

        mockMvc.perform(postZone("{\"name\": \"D\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zoneId").value(4))
                .andExpect(jsonPath("$.name").value("D"));
    }

    @Test
    @DisplayName("TC-ZON-H04 ชื่อว่าง / มีแต่เว้นวรรค → 400")
    void create_blank() throws Exception {
        mockMvc.perform(postZone("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("กรุณากรอกชื่อของโซน"));
        mockMvc.perform(postZone("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(zoneService);
    }

    @Test
    @DisplayName("TC-ZON-H05 ชื่อยาว 51 ตัว → 400 (คอลัมน์เป็น VARCHAR(50))")
    void create_tooLong() throws Exception {
        mockMvc.perform(postZone("{\"name\": \"" + "x".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
        verifyNoInteractions(zoneService);
    }

    @Test
    @DisplayName("TC-ZON-H06 ชื่อซ้ำ (uk_zone_name) → 409 ZONE_NAME_TAKEN")
    void create_duplicate() throws Exception {
        when(zoneService.createZone(any())).thenThrow(new DataIntegrityViolationException(
                "Duplicate entry 'A' for key 'zone.uk_zone_name'"));

        mockMvc.perform(postZone("{\"name\": \"A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ZONE_NAME_TAKEN"));
    }
}
