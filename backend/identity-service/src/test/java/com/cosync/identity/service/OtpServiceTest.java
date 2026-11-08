package com.cosync.identity.service;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.entity.EmailOtp;
import com.cosync.identity.entity.User;
import com.cosync.identity.mail.MailService;
import com.cosync.identity.repository.EmailOtpRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String CORRECT_CODE = "123456";

    @Mock EmailOtpRepository emailOtpRepository;   // ของปลอม ไม่ต่อ DB
    @Mock MailService mailService;                 // ของปลอม ไม่ส่งอีเมลจริง

    // ใช้ของจริง (strength 4 = เร็ว เหมาะกับ test)
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private OtpService otpService;
    private User user;

    @BeforeEach
    void setUp() {
        // ⚠️ ลำดับพารามิเตอร์ต้องตรงกับ constructor ของ OtpService ในโปรเจกต์คุณ
        //    ttl-minutes = 10, max-attempts = 5, resend-cooldown-seconds = 60
        otpService = new OtpService(emailOtpRepository, passwordEncoder, mailService, 10, 5, 60);
        user = mock(User.class);
    }

    /** สร้าง OTP แล้วตั้งให้ repository คืนตัวนี้ เมื่อ service ถามหา OTP ล่าสุด */
    private EmailOtp givenLatestOtp(Instant expiresAt) {
        EmailOtp otp = new EmailOtp(user, passwordEncoder.encode(CORRECT_CODE), expiresAt);
        when(emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));
        return otp;
    }

    private static Instant inMinutes(long minutes) {
        return Instant.now().plusSeconds(minutes * 60);
    }

    // ---------------------------------------------------------------

    @Test
    @DisplayName("รหัสถูก → ผ่าน และ OTP ถูกใช้ไปแล้ว")
    void correctCode_marksOtpUsed() {
        // Arrange: เตรียม
        EmailOtp otp = givenLatestOtp(inMinutes(10));

        // Act: เรียกเมธอดที่ทดสอบ
        assertDoesNotThrow(() -> otpService.verify(user, CORRECT_CODE));

        // Assert: เช็กผล
        assertTrue(otp.isUsed());
    }

    @Test
    @DisplayName("รหัสผิด → OTP_INVALID และนับครั้งที่ผิดเพิ่ม 1")
    void wrongCode_countsAttempt() {
        EmailOtp otp = givenLatestOtp(inMinutes(10));

        ApiException e = assertThrows(ApiException.class,
                () -> otpService.verify(user, "999999"));

        assertEquals("OTP_INVALID", e.getCode());
        assertEquals(1, otp.getAttempts());
    }

    @Test
    @DisplayName("หมดอายุ → OTP_EXPIRED แม้รหัสถูก")
    void expired_rejected() {
        givenLatestOtp(inMinutes(-1));   // หมดอายุไปแล้ว 1 นาที

        ApiException e = assertThrows(ApiException.class,
                () -> otpService.verify(user, CORRECT_CODE));

        assertEquals("OTP_EXPIRED", e.getCode());
    }

    @Test
    @DisplayName("ผิดครบ 5 ครั้ง → OTP_LOCKED แม้ครั้งนี้รหัสถูก")
    void tooManyAttempts_locked() {
        EmailOtp otp = givenLatestOtp(inMinutes(10));
        for (int i = 0; i < 5; i++) {
            otp.recordFailedAttempt();
        }

        ApiException e = assertThrows(ApiException.class,
                () -> otpService.verify(user, CORRECT_CODE));

        assertEquals("OTP_LOCKED", e.getCode());
    }

    @Test
    @DisplayName("ใช้ไปแล้ว → ใช้ซ้ำไม่ได้")
    void alreadyUsed_rejected() {
        EmailOtp otp = givenLatestOtp(inMinutes(10));
        otp.markUsed();

        ApiException e = assertThrows(ApiException.class,
                () -> otpService.verify(user, CORRECT_CODE));

        assertEquals("OTP_LOCKED", e.getCode());
    }

    @Test
    @DisplayName("ไม่เคยขอ OTP → OTP_INVALID")
    void noOtp_rejected() {
        when(emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class,
                () -> otpService.verify(user, CORRECT_CODE));

        assertEquals("OTP_INVALID", e.getCode());
    }
}