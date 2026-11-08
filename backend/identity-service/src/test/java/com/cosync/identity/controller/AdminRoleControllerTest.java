package com.cosync.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cosync.identity.dto.role.PermissionResponse;
import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.service.RoleService;

@WebMvcTest({ AdminRoleController.class, AdminPermissionController.class })
class AdminRoleControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RoleService roleService;

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header("X-User-Id", "1")
                .header("X-User-Type", "STAFF")
                .header("X-Permissions", "role.manage,employee.manage");
    }

    @Test
    @DisplayName("TC-ROL-01 (HTTP) Admin ดู role → 200")
    void listRoles() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(new RoleResponse(1L, "Admin", Set.of("role.manage"))));

        mockMvc.perform(asAdmin(get("/admin/roles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Admin"));
    }

    @Test
    @DisplayName("TC-ROL-02 (HTTP) /admin/permissions ต้องมี role.manage")
    void listPermissions_requiresRoleManage() throws Exception {
        when(roleService.listPermissions()).thenReturn(List.of(new PermissionResponse("report.view", "View reports")));

        mockMvc.perform(asAdmin(get("/admin/permissions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("report.view"));

        mockMvc.perform(get("/admin/permissions")
                        .header("X-User-Type", "STAFF").header("X-Permissions", "employee.manage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ROL-03 (HTTP) สร้าง role → 201")
    void createRole_created() throws Exception {
        when(roleService.create(any())).thenReturn(new RoleResponse(7L, "Shift Lead", Set.of("report.view")));

        mockMvc.perform(asAdmin(post("/admin/roles")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shift Lead\",\"permissions\":[\"report.view\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleId").value(7));
    }

    @Test
    @DisplayName("TC-ROL-07 ไม่ส่ง permissions → 400 details.permissions")
    void createRole_missingPermissions() throws Exception {
        mockMvc.perform(asAdmin(post("/admin/roles")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Perms\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.permissions").exists());

        verifyNoInteractions(roleService);
    }

    @Test
    @DisplayName("TC-ROL-09 (HTTP) PUT ส่ง roleId จาก URL ให้ service")
    void updateRole_passesId() throws Exception {
        when(roleService.update(eq(7L), any())).thenReturn(new RoleResponse(7L, "Shift Lead", Set.of()));

        mockMvc.perform(asAdmin(put("/admin/roles/7")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shift Lead\",\"permissions\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(7));
    }
}
