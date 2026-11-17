package com.cosync.wallet.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.cosync.wallet.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * กั้น /internal/** ให้เรียกได้เฉพาะ service ด้วยกัน (ตอนนี้คือ identity ตอนสมัครสมาชิก) ที่ส่ง X-Internal-Token ถูก
 * เทียบด้วย MessageDigest.isEqual (constant-time) กันการเดา token จากเวลาที่ใช้ตอบ
 */
@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    private final byte[] token;

    public InternalTokenInterceptor(@Value("${app.internal.token}") String token) {
        if (token.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_TOKEN ว่าง ตั้งค่าใน .env ก่อน");
        }
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sent = request.getHeader("X-Internal-Token");
        if (sent == null || !MessageDigest.isEqual(token, sent.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_TOKEN", "ไม่มีสิทธิ์เรียก API ภายใน");
        }
        return true;
    }
}
