package com.cosync.identity.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.cosync.identity.entity.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    public static final String COOKIE_NAME = "access_token";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-hours}") int ttlHours) {
        // แปลงข้อความ secret เป็นกุญแจสำหรับ HS256 (ยาวไม่ถึง 32 byte จะ error ตอนเริ่มแอป)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(ttlHours);
    }

    /** สร้าง JWT ของสมาชิก */
    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
        .subject(user.getId().toString())
        .claim("typ", "MEMBER")
        .issuedAt(java.util.Date.from(now))
        .expiration(java.util.Date.from(now.plus(ttl)))
        .signWith(key)
        .compact();
    }

    public ResponseCookie createCookie(String token) {
        return buildCookie(token, ttl);
    }
    
    public ResponseCookie clearCookie() {
        return buildCookie("", Duration.ZERO);
    }
    
    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(false)      // ตอนขึ้น HTTPS จริงเปลี่ยนเป็น true
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}