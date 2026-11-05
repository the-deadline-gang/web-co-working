package com.cosync.identity.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.login.LoginRequest;
import com.cosync.identity.dto.login.LoginResult;
import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.dto.otp.VerifyOtpRequest;
import com.cosync.identity.dto.register.RegisterRequest;
import com.cosync.identity.dto.register.RegisterResponse;
import com.cosync.identity.entity.User;
import com.cosync.identity.repository.UserRepository;
import com.cosync.identity.security.JwtService;


@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder , OtpService otpService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "อีเมลนี้ถูกใช้สมัครแล้ว");
        }
        if (userRepository.existsByPhoneNumber(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "เบอร์โทรนี้ถูกใช้สมัครแล้ว");
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone(),
                passwordEncoder.encode(request.password()),
                randomHex(32)); // qrcode

        User saved = userRepository.save(user);

       
        Instant otpExpiresAt = otpService.issue(saved); 
        return new RegisterResponse(saved.getId(), otpExpiresAt);
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "รหัสไม่ถูกต้อง"));
        otpService.verify(user, request.otp());
        user.markEmailVerified();
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        // 1–2: หา user และเทียบรหัสผ่าน
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
        .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "อีเมลหรือรหัสผ่านไม่ถูกต้อง" ));

         // 3: ยืนยันอีเมลแล้วหรือยัง
        if(user.isEmailVerified() == false){
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED", "กรุณายืนยันอีเมลก่อนเข้าสู่ระบบ");
        }
        // 4: ถูกระงับไหม
        if(!user.isActive()){
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_SUSPENDED", "บัญชีผู้ใช้นี้ถูกระงับ");
        }
        // 5: บันทึกเวลา login
        user.recordLogin();
        // 6: สร้าง token แล้วคืน new LoginResult(token, MeResponse.from(user))
        String token = jwtService.createToken(user);

        return  new LoginResult(token, MeResponse.from(user));
    }
   
}