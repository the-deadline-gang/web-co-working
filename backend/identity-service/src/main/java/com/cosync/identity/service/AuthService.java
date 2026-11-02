package com.cosync.identity.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.register.RegisterRequest;
import com.cosync.identity.dto.register.RegisterResponse;
import com.cosync.identity.entity.User;
import com.cosync.identity.repository.UserRepository;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

       
        Instant otpExpiresAt = Instant.now().plusSeconds(10 * 60);

        return new RegisterResponse(saved.getId(), otpExpiresAt);
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }
}