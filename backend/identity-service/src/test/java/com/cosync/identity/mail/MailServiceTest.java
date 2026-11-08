package com.cosync.identity.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("TC-REG-01 อีเมล OTP: ส่งถึงผู้สมัคร มีรหัสและอายุของรหัส")
    void sendOtp() {
        mailService.sendOtp("nan@example.com", "482913", 10);

        SimpleMailMessage message = sentMessage();
        assertArrayEquals(new String[] { "nan@example.com" }, message.getTo());
        assertEquals("no-reply@cosync.local", message.getFrom());
        assertTrue(message.getText().contains("482913"));
        assertTrue(message.getText().contains("10"));
    }

    @Test
    @DisplayName("TC-EMP-01 อีเมลต้อนรับพนักงาน: มีอีเมล login และรหัสชั่วคราว")
    void sendWelcome() {
        mailService.sendWelcome("counter9@cosync.local", "kHHNExxRkfQz");

        SimpleMailMessage message = sentMessage();
        assertArrayEquals(new String[] { "counter9@cosync.local" }, message.getTo());
        assertTrue(message.getText().contains("counter9@cosync.local"));
        assertTrue(message.getText().contains("kHHNExxRkfQz"));
        assertEquals("บัญชีพนักงาน CO-SYNC ของคุณ", message.getSubject());
    }
}
