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


    public void sendWelcome(String to, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cosync.local");
        message.setTo(to);
        message.setSubject("Welcome to CO-SYNC");
        message.setSubject("บัญชีพนักงาน CO-SYNC ของคุณ");
        message.setText("""
                ยินดีต้อนรับสู่ CO-SYNC

                อีเมลสำหรับเข้าสู่ระบบ: %s
                รหัสผ่านชั่วคราว: %s

                กรุณาเปลี่ยนรหัสผ่านหลังเข้าสู่ระบบครั้งแรก
                """.formatted(to, tempPassword));
        mailSender.send(message);
    }
}