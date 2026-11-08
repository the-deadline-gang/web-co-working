package com.cosync.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.cosync.identity.dto.employee.EmployeeResponse;
import com.cosync.identity.entity.EmployeeStatus;
import com.cosync.identity.service.EmployeeService;

@WebMvcTest(AdminEmployeeController.class)
class AdminEmployeeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EmployeeService employeeService;

    private static final String VALID_BODY =
            "{\"firstName\":\"สมชาย\",\"lastName\":\"ใจดี\",\"email\":\"counter9@cosync.local\",\"roleId\":5}";

    private static final EmployeeResponse COUNTER9 = new EmployeeResponse(
            9L, "STF-009", "สมชาย", "ใจดี", "counter9@cosync.local", null, 5L, "Counter", "ACTIVE");

    /** ใส่ header แบบที่ Gateway ใส่ให้ Admin */
    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header("X-User-Id", "1")
                .header("X-User-Type", "STAFF")
                .header("X-Permissions", "role.manage,employee.manage");
    }

    @Test
    @DisplayName("TC-EMP-01 (HTTP) Admin สร้างพนักงาน → 201")
    void create_created() throws Exception {
        when(employeeService.create(any())).thenReturn(COUNTER9);

        mockMvc.perform(asAdmin(post("/admin/employees")).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayId").value("STF-009"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("TC-EMP-05 (HTTP) ไม่มี roleId และอีเมลผิด → 400 บอกครบทุกช่อง")
    void create_invalidBody() throws Exception {
        mockMvc.perform(asAdmin(post("/admin/employees")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"x\",\"email\":\"not-an-email\",\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.phone").exists())
                .andExpect(jsonPath("$.details.roleId").exists());

        verifyNoInteractions(employeeService);
    }

    @Test
    @DisplayName("TC-SEC-07 Counter (มีแค่ loan.record) สร้างพนักงาน → 403")
    void create_counter_forbidden() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .header("X-User-Id", "2").header("X-User-Type", "STAFF")
                        .header("X-Permissions", "loan.record,checkout.override")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(employeeService);
    }

    @Test
    @DisplayName("TC-SEC-11 สมาชิกส่ง body ผิดมา → 403 ก่อน 400 (ไม่บอกว่าต้องส่งอะไร)")
    void create_memberWithBadBody_forbiddenBeforeValidation() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .header("X-User-Id", "4").header("X-User-Type", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-EMP-07 (HTTP) ไม่ใส่ status → ส่ง null ให้ service")
    void list_all() throws Exception {
        when(employeeService.list(null)).thenReturn(List.of(COUNTER9));

        mockMvc.perform(asAdmin(get("/admin/employees")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(9));
    }

    @Test
    @DisplayName("TC-EMP-08 (HTTP) ?status=SUSPENDED → แปลงเป็น enum ให้ service")
    void list_byStatus() throws Exception {
        when(employeeService.list(EmployeeStatus.SUSPENDED)).thenReturn(List.of());

        mockMvc.perform(asAdmin(get("/admin/employees")).param("status", "SUSPENDED"))
                .andExpect(status().isOk());

        verify(employeeService).list(EmployeeStatus.SUSPENDED);
    }

    @Test
    @DisplayName("TC-EMP-09 ?status=XYZ → 400 VALIDATION_FAILED (ไม่ใช่ 500)")
    void list_invalidStatus() throws Exception {
        mockMvc.perform(asAdmin(get("/admin/employees")).param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(employeeService);
    }

    @Test
    @DisplayName("TC-EMP-13 (HTTP) ส่ง id จาก URL และ X-User-Id ให้ service ถูกตัว")
    void update_passesIdsFromPathAndHeader() throws Exception {
        when(employeeService.update(eq(2L), eq(1L), any())).thenReturn(COUNTER9);

        mockMvc.perform(asAdmin(put("/admin/employees/2")).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        verify(employeeService).update(eq(2L), eq(1L), any());
    }

    @Test
    @DisplayName("PUT ด้วย id ที่ไม่ใช่ตัวเลข → 400 VALIDATION_FAILED")
    void update_nonNumericId() throws Exception {
        mockMvc.perform(asAdmin(put("/admin/employees/abc")).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
