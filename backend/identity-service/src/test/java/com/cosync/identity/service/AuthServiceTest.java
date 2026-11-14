package com.cosync.identity.service;

import static com.cosync.identity.testsupport.TestData.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.cosync.identity.client.WalletClient;
import com.cosync.identity.common.ApiException;
import com.cosync.identity.common.WalletUnavailableException;
import com.cosync.identity.dto.login.LoginRequest;
import com.cosync.identity.dto.login.LoginResult;
import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.dto.login.StaffLoginResult;
import com.cosync.identity.dto.otp.ResendOtpRequest;
import com.cosync.identity.dto.otp.VerifyOtpRequest;
import com.cosync.identity.dto.register.RegisterRequest;
import com.cosync.identity.dto.register.RegisterResponse;
import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.EmployeeStatus;
import com.cosync.identity.entity.Role;
import com.cosync.identity.entity.User;
import com.cosync.identity.repository.EmployeeRepository;
import com.cosync.identity.repository.UserRepository;
import com.cosync.identity.security.JwtService;
import com.cosync.identity.testsupport.TestData;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "P@ssw0rd123";
    private static final String EMAIL = "verify3@example.com";

    @Mock UserRepository userRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock OtpService otpService;
    @Mock WalletClient walletClient;

    private final JwtService jwtService = new JwtService(TestData.JWT_SECRET, 12);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, TestData.PASSWORD_ENCODER, otpService, jwtService, employeeRepository, walletClient);
    }

    private static RegisterRequest registerRequest(String email, String phone) {
        return new RegisterRequest("ทดสอบ", "ระบบ", email, phone, PASSWORD, true);
    }

    private static ApiException assertApiError(String code, HttpStatus status, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(code, e.getCode());
        assertEquals(status, e.getStatus());
        return e;
    }

    // =================== สมัคร ===================

    @Test
    @DisplayName("TC-REG-01 สมัครสำเร็จ: เก็บอีเมลตัวเล็ก, hash รหัสผ่าน, ออก OTP")
    void register_success() {
        when(userRepository.existsByEmail("nan@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0811111111")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 7L));
        Instant expiry = Instant.now().plusSeconds(600);
        when(otpService.issue(any(User.class))).thenReturn(expiry);

        RegisterResponse response = authService.register(registerRequest(" Nan@Example.com ", "0811111111"));

        assertEquals(7L, response.userId());
        assertEquals(expiry, response.otpExpiresAt());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("nan@example.com", user.getEmail());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(TestData.PASSWORD_ENCODER.matches(PASSWORD, user.getPasswordHash()));
        assertEquals(64, user.getQrSecret().length());
        assertFalse(user.isEmailVerified());
    }

    @Test
    @DisplayName("TC-REG-02 อีเมลซ้ำ → 409 EMAIL_TAKEN")
    void register_emailTaken() {
        when(userRepository.existsByEmail("nan@example.com")).thenReturn(true);

        assertApiError("EMAIL_TAKEN", HttpStatus.CONFLICT,
                () -> authService.register(registerRequest("nan@example.com", "0811111111")));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("TC-REG-03 อีเมลซ้ำต่างตัวพิมพ์/มีช่องว่าง → ปรับก่อนเช็ก แล้วได้ 409")
    void register_emailTakenDifferentCase() {
        when(userRepository.existsByEmail("nan@example.com")).thenReturn(true);

        assertApiError("EMAIL_TAKEN", HttpStatus.CONFLICT,
                () -> authService.register(registerRequest(" Nan@Example.COM ", "0811111111")));
    }

    @Test
    @DisplayName("TC-REG-04 เบอร์ซ้ำ → 409 PHONE_TAKEN")
    void register_phoneTaken() {
        when(userRepository.existsByEmail("nan@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0811111111")).thenReturn(true);

        assertApiError("PHONE_TAKEN", HttpStatus.CONFLICT,
                () -> authService.register(registerRequest("nan@example.com", "0811111111")));

        verify(userRepository, never()).save(any());
    }

    // =================== ยืนยัน OTP ===================

    @Test
    @DisplayName("TC-OTP-01 (ฝั่ง AuthService) OTP ผ่าน → บัญชียืนยันแล้ว")
    void verifyOtp_success_marksVerified() {
        User user = TestData.user(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.verifyOtp(new VerifyOtpRequest(EMAIL, "123456"));

        verify(otpService).verify(user, "123456");
        assertTrue(user.isEmailVerified());
        verify(walletClient).createWallet(4L);          // W-03: ยืนยันแล้วต้องสร้าง wallet
    }

    @Test
    @DisplayName("TC-OTP-02 (ฝั่ง AuthService) OTP ไม่ผ่าน → บัญชียังไม่ยืนยัน")
    void verifyOtp_wrongCode_notVerified() {
        User user = TestData.user(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "รหัสไม่ถูกต้อง"))
                .when(otpService).verify(user, "000000");

        assertApiError("OTP_INVALID", HttpStatus.BAD_REQUEST,
                () -> authService.verifyOtp(new VerifyOtpRequest(EMAIL, "000000")));

        assertFalse(user.isEmailVerified());
        verifyNoInteractions(walletClient);              // OTP ผิด → ไม่ต้องไปสร้าง wallet
    }

    @Test
    @DisplayName("TC-OTP-09 W-03 wallet ไม่ตอบ → โยน WalletUnavailableException ออกไป (503)")
    void verifyOtp_walletDown_throws() {
        User user = TestData.user(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        doThrow(new WalletUnavailableException()).when(walletClient).createWallet(4L);

        assertThrows(WalletUnavailableException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(EMAIL, "123456")));
    }

    @Test
    @DisplayName("TC-OTP-10 W-03 WalletUnavailableException ต้องไม่ใช่ลูกของ ApiException ไม่อย่างนั้น noRollbackFor จะไม่ rollback")
    void walletUnavailable_isNotApiException() {
        assertFalse(ApiException.class.isAssignableFrom(WalletUnavailableException.class));
    }

    @Test
    @DisplayName("TC-OTP-07 อีเมลไม่มีในระบบ → OTP_INVALID เหมือนรหัสผิด")
    void verifyOtp_unknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertApiError("OTP_INVALID", HttpStatus.BAD_REQUEST,
                () -> authService.verifyOtp(new VerifyOtpRequest("ghost@example.com", "123456")));

        verifyNoInteractions(otpService);
    }

    // =================== ขอ OTP ใหม่ ===================

    @Test
    @DisplayName("TC-RES-01 (ฝั่ง AuthService) ยังไม่ยืนยัน → ส่งต่อให้ OtpService.resend")
    void resend_unverified_delegates() {
        User user = TestData.user(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.resendOtp(new ResendOtpRequest(" Verify3@Example.com "));

        verify(otpService).resend(user);
    }

    @Test
    @DisplayName("TC-RES-03 อีเมลไม่มีในระบบ → จบเงียบๆ ไม่ส่ง OTP")
    void resend_unknownEmail_silent() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.resendOtp(new ResendOtpRequest("ghost@example.com"));

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("TC-RES-04 ยืนยันแล้ว → จบเงียบๆ ไม่ส่ง OTP")
    void resend_verified_silent() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.resendOtp(new ResendOtpRequest(EMAIL));

        verifyNoInteractions(otpService);
    }

    // =================== Login สมาชิก ===================

    @Test
    @DisplayName("TC-LOG-01 login สำเร็จ → ได้ token + Me และบันทึก last_login_at")
    void login_success() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        LoginResult result = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertNotNull(result.token());
        assertEquals("U-000004", result.me().displayId());
        assertEquals(EMAIL, result.me().email());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    @DisplayName("TC-LOG-02 รหัสผิด → 401 BAD_CREDENTIALS")
    void login_wrongPassword() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")));

        assertNull(user.getLastLoginAt());
    }

    @Test
    @DisplayName("TC-LOG-03 อีเมลไม่มี → BAD_CREDENTIALS ข้อความเดียวกับรหัสผิด")
    void login_unknownEmail_sameAsWrongPassword() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ApiException wrongPassword = assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")));
        ApiException unknownEmail = assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest("ghost@example.com", PASSWORD)));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    @DisplayName("TC-LOG-04 ยังไม่ยืนยันอีเมล → 401 EMAIL_NOT_VERIFIED")
    void login_notVerified() {
        User user = TestData.user(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertApiError("EMAIL_NOT_VERIFIED", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD)));
    }

    @Test
    @DisplayName("TC-LOG-05 บัญชีถูกระงับ → 401 ACCOUNT_SUSPENDED")
    void login_suspended() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        TestData.suspend(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertApiError("ACCOUNT_SUSPENDED", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD)));
    }

    @Test
    @DisplayName("TC-LOG-06 ระงับ + รหัสผิด → BAD_CREDENTIALS (ไม่เผยว่าถูกระงับ)")
    void login_suspendedWrongPassword() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        TestData.suspend(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")));
    }

    @Test
    @DisplayName("TC-LOG-07 อีเมลตัวใหญ่/มีช่องว่าง → login ได้")
    void login_normalizesEmail() {
        User user = TestData.verifiedUser(4L, EMAIL, hash(PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        LoginResult result = authService.login(new LoginRequest(" Verify3@Example.com ", PASSWORD));

        assertNotNull(result.token());
    }

    // =================== /me ===================

    @Test
    @DisplayName("TC-ME-01 (ฝั่ง service) คืนข้อมูลพร้อม displayId U-xxxxxx")
    void getMe_found() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(TestData.user(4L, EMAIL, hash(PASSWORD))));

        MeResponse me = authService.getMe(4L);

        assertEquals("U-000004", me.displayId());
    }

    @Test
    @DisplayName("TC-ME-03 ไม่พบ user → 404 USER_NOT_FOUND")
    void getMe_notFound() {
        when(userRepository.findById(99999L)).thenReturn(Optional.empty());

        assertApiError("USER_NOT_FOUND", HttpStatus.NOT_FOUND, () -> authService.getMe(99999L));
    }

    // =================== Staff login ===================

    private Employee admin() {
        Role role = TestData.role(1L, "Admin",
                TestData.permission(1L, "role.manage"),
                TestData.permission(2L, "employee.manage"));
        return TestData.employee(1L, role, "admin@cosync.local", hash("Admin@1234"));
    }

    @Test
    @DisplayName("TC-SLG-01 staff login สำเร็จ → STF-001 + สิทธิ์ของ role")
    void staffLogin_success() {
        Employee admin = admin();
        when(employeeRepository.findByEmail("admin@cosync.local")).thenReturn(Optional.of(admin));

        StaffLoginResult result = authService.staffLogin(new LoginRequest("admin@cosync.local", "Admin@1234"));

        assertNotNull(result.token());
        assertEquals("STF-001", result.me().displayId());
        assertEquals("Admin", result.me().role());
        assertEquals(Set.of("role.manage", "employee.manage"), result.me().permissions());
        assertNotNull(admin.getLastLoginAt());
    }

    @Test
    @DisplayName("TC-SLG-02 staff รหัสผิด → 401 BAD_CREDENTIALS")
    void staffLogin_wrongPassword() {
        when(employeeRepository.findByEmail("admin@cosync.local")).thenReturn(Optional.of(admin()));

        assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.staffLogin(new LoginRequest("admin@cosync.local", "wrong")));
    }

    @Test
    @DisplayName("TC-SLG-03 สมาชิกเอาอีเมลมา login ฝั่งพนักงาน → 401 (ค้นแค่ตาราง employee)")
    void staffLogin_memberEmail() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertApiError("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                () -> authService.staffLogin(new LoginRequest(EMAIL, PASSWORD)));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("TC-SLG-04 พนักงานถูกระงับ → 401 ACCOUNT_SUSPENDED")
    void staffLogin_suspended() {
        Employee admin = admin();
        admin.changeStatus(EmployeeStatus.SUSPENDED);
        when(employeeRepository.findByEmail("admin@cosync.local")).thenReturn(Optional.of(admin));

        assertApiError("ACCOUNT_SUSPENDED", HttpStatus.UNAUTHORIZED,
                () -> authService.staffLogin(new LoginRequest("admin@cosync.local", "Admin@1234")));

        assertNull(admin.getLastLoginAt());
    }
}
