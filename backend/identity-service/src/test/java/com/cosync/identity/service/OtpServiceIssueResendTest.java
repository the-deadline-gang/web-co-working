package com.cosync.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.entity.EmailOtp;
import com.cosync.identity.entity.User;
import com.cosync.identity.mail.MailService;
import com.cosync.identity.repository.EmailOtpRepository;
import com.cosync.identity.testsupport.TestData;

/** ส่วน issue() และ resend() ของ OtpService (verify() อยู่ใน OtpServiceTest) */
@ExtendWith(MockitoExtension.class)
class OtpServiceIssueResendTest {

    private static final int TTL_MINUTES = 10;
    private static final int COOLDOWN_SECONDS = 60;

    @Mock EmailOtpRepository emailOtpRepository;
    @Mock MailService mailService;

    private OtpService otpService;
    private User user;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(emailOtpRepository, TestData.PASSWORD_ENCODER, mailService,
                TTL_MINUTES, 5, COOLDOWN_SECONDS);
        user = TestData.user(4L, "verify4@example.com", "hash");
    }

    /** OTP ล่าสุดที่สร้างไว้เมื่อ secondsAgo วินาทีก่อน */
    private void givenLatestOtpCreated(long secondsAgo) {
        EmailOtp latest = new EmailOtp(user, "old-hash", Instant.now().plusSeconds(600));
        ReflectionTestUtils.setField(latest, "createdAt", Instant.now().minusSeconds(secondsAgo));
        when(emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(latest));
    }

    @Test
    @DisplayName("TC-REG-01 (ฝั่ง OTP) issue: เก็บแค่ hash, ส่งรหัส 6 หลักทางอีเมล, หมดอายุใน 10 นาที")
    void issue_savesHashAndSendsPlainCode() {
        Instant before = Instant.now();

        Instant expiresAt = otpService.issue(user);

        ArgumentCaptor<EmailOtp> saved = ArgumentCaptor.forClass(EmailOtp.class);
        verify(emailOtpRepository).save(saved.capture());
        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtp(eq("verify4@example.com"), sentCode.capture(), eq(TTL_MINUTES));

        String code = sentCode.getValue();
        assertTrue(code.matches("[0-9]{6}"), "OTP ต้องเป็นตัวเลข 6 หลัก แต่ได้ " + code);
        assertNotEquals(code, saved.getValue().getCodeHash());
        assertTrue(TestData.PASSWORD_ENCODER.matches(code, saved.getValue().getCodeHash()));

        Duration ttl = Duration.between(before, expiresAt);
        assertTrue(ttl.compareTo(Duration.ofMinutes(TTL_MINUTES).minusSeconds(5)) > 0);
        assertTrue(ttl.compareTo(Duration.ofMinutes(TTL_MINUTES).plusSeconds(5)) < 0);
        assertEquals(expiresAt, saved.getValue().getExpiresAt());
    }

    @Test
    @DisplayName("TC-RES-01 ครบ cooldown แล้ว → ออก OTP ใหม่")
    void resend_afterCooldown_issuesNewOtp() {
        givenLatestOtpCreated(COOLDOWN_SECONDS + 1);

        otpService.resend(user);

        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(mailService).sendOtp(eq("verify4@example.com"), any(), eq(TTL_MINUTES));
    }

    @Test
    @DisplayName("TC-RES-02 ขอซ้ำภายใน 60 วินาที → 429 OTP_COOLDOWN ไม่ส่งอีเมล")
    void resend_withinCooldown_rejected() {
        givenLatestOtpCreated(10);

        ApiException e = assertThrows(ApiException.class, () -> otpService.resend(user));

        assertEquals("OTP_COOLDOWN", e.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        verify(emailOtpRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("ไม่เคยมี OTP มาก่อน → ขอได้ทันที ไม่ติด cooldown")
    void resend_noPreviousOtp_issues() {
        when(emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());

        otpService.resend(user);

        verify(mailService).sendOtp(eq("verify4@example.com"), any(), eq(TTL_MINUTES));
    }
}
