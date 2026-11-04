package com.cosync.identity.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class OtpMailer {

    private final JavaMailSender mailSender;

    public OtpMailer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String otp, int ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cosync.local");
        message.setTo(to);
        message.setSubject("รหัสยืนยันอีเมล CO-SYNC");
        message.setText("รหัสยืนยันของคุณคือ " + otp + "\nใช้ได้ภายใน " + ttlMinutes + " นาที");
        mailSender.send(message);
    }
}