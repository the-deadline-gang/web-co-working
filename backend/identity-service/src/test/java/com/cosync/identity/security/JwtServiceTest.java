package com.cosync.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.Role;
import com.cosync.identity.testsupport.TestData;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TestData.JWT_SECRET, 12);

    /** อ่าน token ด้วย secret เดียวกับที่ Gateway ใช้ */
    private static Claims parse(String token, String secret) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("TC-LOG-01 token สมาชิก: sub = userId, typ = MEMBER, อายุ 12 ชั่วโมง")
    void memberToken_claims() {
        String token = jwtService.createToken(TestData.user(4L, "verify3@example.com", "hash"));

        Claims claims = parse(token, TestData.JWT_SECRET);

        assertEquals("4", claims.getSubject());
        assertEquals("MEMBER", claims.get("typ", String.class));
        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(Duration.ofHours(12).toMillis(), lifetimeMillis);
    }

    @Test
    @DisplayName("TC-SLG-01 token พนักงาน: typ = STAFF และมี perms ของ role")
    void staffToken_claims() {
        Role counter = TestData.role(5L, "Counter",
                TestData.permission(8L, "loan.record"),
                TestData.permission(9L, "checkout.override"));
        Employee employee = TestData.employee(2L, counter, "counter1@cosync.local", "hash");

        Claims claims = parse(jwtService.createStaffToken(employee), TestData.JWT_SECRET);

        assertEquals("2", claims.getSubject());
        assertEquals("STAFF", claims.get("typ", String.class));
        List<?> perms = claims.get("perms", List.class);
        assertEquals(2, perms.size());
        assertTrue(perms.containsAll(List.of("loan.record", "checkout.override")));
    }

    @Test
    @DisplayName("TC-SEC-02 token ที่เซ็นด้วย secret อื่น → ตรวจไม่ผ่าน")
    void tokenFromOtherSecret_rejected() {
        String token = jwtService.createToken(TestData.user(4L, "verify3@example.com", "hash"));

        assertThrows(JwtException.class, () -> parse(token, "another-secret-".repeat(5)));
    }

    @Test
    @DisplayName("TC-LOG-01 cookie: access_token, HttpOnly, SameSite=Lax, Path=/, 12 ชั่วโมง")
    void loginCookie_attributes() {
        ResponseCookie cookie = jwtService.createCookie("abc");

        assertEquals("access_token", cookie.getName());
        assertEquals("abc", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofHours(12), cookie.getMaxAge());
    }

    @Test
    @DisplayName("TC-OUT-01 logout cookie: ค่าว่าง Max-Age=0")
    void logoutCookie_clears() {
        ResponseCookie cookie = jwtService.clearCookie();

        assertEquals("access_token", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
    }
}
