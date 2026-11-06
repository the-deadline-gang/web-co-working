package com.cosync.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/** ตรวจ JWT ทุก request ผ่านแล้วใส่ X-User-Id / X-User-Type ให้ service ข้างหลัง */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/staff/login");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. ลบ header ที่ client อาจปลอมมา
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Type");
                })
                .build();

        // 2. path สาธารณะ  ผ่านเลย
        if (PUBLIC_PATHS.contains(request.getPath().value())) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 3. ไม่มี cookie  401
        HttpCookie cookie = request.getCookies().getFirst("access_token");
        if (cookie == null) {
            return unauthorized(exchange.getResponse());
        }

        // 4. ตรวจ token ไม่ผ่าน  401
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(cookie.getValue())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange.getResponse());
        }

        // 5. ผ่าน  ใส่ตัวตนแล้วส่งต่อ
        ServerHttpRequest withUser = request.mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Type", claims.get("typ", String.class))
                .build();
        return chain.filter(exchange.mutate().request(withUser).build());
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        byte[] body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"กรุณาเข้าสู่ระบบ\"}"
                .getBytes(StandardCharsets.UTF_8);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;   // ทำก่อน filter อื่น
    }
}