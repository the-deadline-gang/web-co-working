package com.cosync.identity.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.employee.ChangePasswordRequest;
import com.cosync.identity.dto.login.LoginResult;
import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.dto.login.StaffLoginResult;
import com.cosync.identity.dto.login.StaffMeResponse;
import com.cosync.identity.dto.register.RegisterResponse;
import com.cosync.identity.security.JwtService;
import com.cosync.identity.service.AuthService;
import com.cosync.identity.service.EmployeeService;

/**
 * ทดสอบชั้น HTTP ของ /auth/** โดยไม่ต้องมี DB
 * Spring เปิดแค่ controller + GlobalExceptionHandler + PermissionInterceptor ส่วน service เป็นของปลอม
 */
@WebMvcTest(AuthController.class)
@Import(JwtService.class)
@TestPropertySource(properties = {
        "app.jwt.secret=webmvc-test-secret-0123456789-0123456789-0123456789",
        "app.jwt.ttl-hours=12" })
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean EmployeeService employeeService;

    private static final String VALID_REGISTER = """
            {"firstName":"ทดสอบ","lastName":"ระบบ","email":"nan@example.com",
             "phone":"0811111111","password":"P@ssw0rd123","acceptTerms":true}
            """;

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // =================== สมัคร ===================

    @Test
    @DisplayName("TC-REG-01 (HTTP) สมัครสำเร็จ → 201 + userId")
    void register_created() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse(7L, Instant.now().plusSeconds(600)));

        postJson("/auth/register", VALID_REGISTER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.otpExpiresAt").exists());
    }

    @Test
    @DisplayName("TC-REG-05 ไม่ยอมรับเงื่อนไข → 400 details.acceptTerms ไม่เรียก service")
    void register_termsNotAccepted() throws Exception {
        postJson("/auth/register", VALID_REGISTER.replace("\"acceptTerms\":true", "\"acceptTerms\":false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.acceptTerms").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("TC-REG-06 รหัสผ่าน 7 ตัว และ 73 ตัว → 400 details.password")
    void register_passwordLength() throws Exception {
        postJson("/auth/register", VALID_REGISTER.replace("P@ssw0rd123", "Short1!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());

        postJson("/auth/register", VALID_REGISTER.replace("P@ssw0rd123", "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("TC-REG-07 เบอร์ผิดรูปแบบ (9 หลัก / ไม่ขึ้นต้นด้วย 0 / มีตัวอักษร) → 400 details.phone")
    void register_invalidPhone() throws Exception {
        for (String phone : new String[] { "081111111", "1811111111", "08111abcde" }) {
            postJson("/auth/register", VALID_REGISTER.replace("0811111111", phone))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.phone").exists());
        }
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("TC-REG-08 JSON พัง → 400 VALIDATION_FAILED (ไม่ใช่ 500)")
    void register_malformedJson() throws Exception {
        postJson("/auth/register", "{\"email\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("TC-REG-02 (HTTP) service โยน ApiException → ได้ status และ code ตามนั้น")
    void register_businessErrorPassesThrough() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "อีเมลนี้ถูกใช้สมัครแล้ว"));

        postJson("/auth/register", VALID_REGISTER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.message").exists());
    }

    // =================== OTP ===================

    @Test
    @DisplayName("TC-OTP-01 (HTTP) ยืนยันสำเร็จ → 204")
    void verifyOtp_noContent() throws Exception {
        postJson("/auth/verify-otp", "{\"email\":\"nan@example.com\",\"otp\":\"123456\"}")
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-OTP-08 OTP ผิดรูปแบบ (5 หลัก / มีตัวอักษร / 7 หลัก) → 400 details.otp")
    void verifyOtp_invalidFormat() throws Exception {
        for (String otp : new String[] { "12345", "abcdef", "0123456" }) {
            postJson("/auth/verify-otp", "{\"email\":\"nan@example.com\",\"otp\":\"" + otp + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.otp").exists());
        }
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("TC-RES-02 (HTTP) cooldown → 429 OTP_COOLDOWN")
    void resendOtp_cooldown() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN", "รอก่อน"))
                .when(authService).resendOtp(any());

        postJson("/auth/resend-otp", "{\"email\":\"nan@example.com\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
    }

    // =================== Login / Logout ===================

    @Test
    @DisplayName("TC-LOG-01 (HTTP) login → 200 + cookie HttpOnly SameSite=Lax 12 ชม. และไม่มีข้อมูลลับใน body")
    void login_setsCookie() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResult("token-abc",
                new MeResponse(4L, "U-000004", "ทดสอบ", "ระบบ", "verify3@example.com", "0800000000")));

        postJson("/auth/login", "{\"email\":\"verify3@example.com\",\"password\":\"P@ssw0rd123\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("access_token=token-abc"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"),
                        containsString("Path=/"),
                        containsString("Max-Age=43200"))))
                .andExpect(jsonPath("$.displayId").value("U-000004"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.qrSecret").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("TC-LOG-02 (HTTP) login ไม่ผ่าน → 401 ไม่มี Set-Cookie")
    void login_failed_noCookie() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "ไม่ถูกต้อง"));

        postJson("/auth/login", "{\"email\":\"verify3@example.com\",\"password\":\"wrong\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("TC-OUT-01 (HTTP) logout → 204 + cookie Max-Age=0")
    void logout_clearsCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("access_token="),
                        containsString("Max-Age=0"))));
    }

    @Test
    @DisplayName("TC-SLG-01 (HTTP) staff login → 200 + cookie + permissions")
    void staffLogin_setsCookie() throws Exception {
        when(authService.staffLogin(any())).thenReturn(new StaffLoginResult("staff-token",
                new StaffMeResponse(1L, "STF-001", "System", "Admin", Set.of("role.manage"))));

        postJson("/auth/staff/login", "{\"email\":\"admin@cosync.local\",\"password\":\"Admin@1234\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("access_token=staff-token")))
                .andExpect(jsonPath("$.permissions[0]").value("role.manage"));
    }

    // =================== เปลี่ยนรหัสพนักงาน ===================

    private ResultActions putPassword(String userType, String json) throws Exception {
        return mockMvc.perform(put("/auth/staff/password")
                .header("X-User-Id", "2")
                .header("X-User-Type", userType)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("TC-PWD-01 (HTTP) พนักงานเปลี่ยนรหัส → 204 และส่ง id จาก header ให้ service")
    void changePassword_staff() throws Exception {
        putPassword("STAFF", "{\"currentPassword\":\"Counter@1234\",\"newPassword\":\"NewPass@5678\"}")
                .andExpect(status().isNoContent());

        verify(employeeService).changePassword(eq(2L), eq(new ChangePasswordRequest("Counter@1234", "NewPass@5678")));
    }

    @Test
    @DisplayName("TC-PWD-04 รหัสใหม่สั้นเกิน → 400 details.newPassword")
    void changePassword_tooShort() throws Exception {
        putPassword("STAFF", "{\"currentPassword\":\"Counter@1234\",\"newPassword\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newPassword").exists());

        verifyNoInteractions(employeeService);
    }

    @Test
    @DisplayName("TC-PWD-05 (HTTP) สมาชิกเรียก → 403 ไม่ถึง service")
    void changePassword_member_forbidden() throws Exception {
        putPassword("MEMBER", "{\"currentPassword\":\"P@ssw0rd123\",\"newPassword\":\"Whatever123\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(employeeService);
    }

    // =================== path ไม่มีอยู่ ===================

    @Test
    @DisplayName("TC-SEC-10 path พิมพ์ผิด → 404 NOT_FOUND (ไม่ใช่ 500)")
    void unknownPath_notFound() throws Exception {
        mockMvc.perform(post("/auth/logou"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
