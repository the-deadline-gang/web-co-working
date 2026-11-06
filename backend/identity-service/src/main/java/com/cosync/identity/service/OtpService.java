package com.cosync.identity.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.entity.EmailOtp;
import com.cosync.identity.entity.User;
import com.cosync.identity.mail.OtpMailer;
import com.cosync.identity.repository.EmailOtpRepository;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailOtpRepository emailOtpRepository;
   ;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailer otpMailer;
    private final int ttlMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;
    public OtpService(EmailOtpRepository emailOtpRepository,
                      PasswordEncoder passwordEncoder,
                      OtpMailer otpMailer,
                      @Value("${app.otp.ttl-minutes}") int ttlMinutes,
                      @Value("${app.otp.max-attempts}") int maxAttempts,
                      @Value("${app.otp.resend-cooldown-seconds}") int resendCooldownSeconds) {
        this.emailOtpRepository = emailOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpMailer = otpMailer;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds; 

    }

    /** สร้าง OTP ใหม่ ส่งอีเมล แล้วคืนเวลาหมดอายุ */
    public Instant issue(User user) {
        String otp = String.valueOf(100000 + RANDOM.nextInt(900000));
        Instant expiresAt = Instant.now().plusSeconds(ttlMinutes * 60L);
        String otpHash = passwordEncoder.encode(otp);

        emailOtpRepository.save(new EmailOtp(user, otpHash, expiresAt));
        otpMailer.send(user.getEmail(), otp, ttlMinutes);

        return expiresAt;
    }

    /** ตรวจ OTP ถ้าไม่ผ่านจะ throw ApiException ถ้าผ่านจะ markUsed() ให้ */
    public void verify(User user, String code) {    
        EmailOtp otp = emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "รหัสไม่ถูกต้อง"));

       if(otp.isUsed() || otp.hasTooManyAttempts(maxAttempts)){
        
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_LOCKED", "รหัสนี้ใช้ไม่ได้แล้ว กรุณาขอรหัสใหม่");
        }

        if(otp.isExpired()){
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "รหัสหมดอายุแล้ว กรุณาขอรหัสใหม่");
        }

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.recordFailedAttempt();
    
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "รหัสไม่ถูกต้อง");
            
        }

        otp.markUsed();
       

    }
    
    /**
     * ขอรหัส OTP ใหม่ ถ้า OTP ล่าสุดยังไม่ครบ 1 นาที จะ throw ApiException
     * @param user ข้อมูลผู้ใช้ที่ต้องการขอ OTP ใหม่
     * @return เวลาหมดอายุของ OTP ใหม่
     */
    public Instant resend(User user) {
        emailOtpRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .filter(latest -> Instant.now().isBefore(latest.getCreatedAt().plusSeconds(resendCooldownSeconds)))
                .ifPresent(latest -> {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN", "กรุณารอสักครู่ก่อนขอรหัสใหม่");
                });
    
        return issue(user);
    }
        

    
     
}
