package com.cosync.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

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

/**
 * ตรวจ JWT ทุก request ก่อนส่งต่อไปยัง service
 * ผ่านแล้วใส่ X-User-Id และ X-User-Type ให้ service ข้างหลังใช้
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String COOKIE_NAME = "access_token";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_TYPE = "X-User-Type";

    /** path ที่ไม่ต้อง login (นับหลังตัด /api ออกแล้ว) */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/register",
            "/auth/verify-otp",
            "/auth/login",
            "/auth/logout");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = removeSpoofedHeaders(exchange.getRequest());

        if (isPublic(request)) {
            return forward(exchange, chain, request);
        }

        Optional<Claims> claims = readToken(request).flatMap(this::verify);
        if (claims.isEmpty()) {
            return unauthorized(exchange.getResponse());
        }

        return forward(exchange, chain, withUserHeaders(request, claims.get()));
    }

    // ---------- แต่ละขั้น ----------

    /** ลบ header ตัวตนที่ browser อาจปลอมส่งมา */
    private ServerHttpRequest removeSpoofedHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_TYPE);
                })
                .build();
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value().replaceFirst("^/api", "");
        return PUBLIC_PATHS.contains(path);
    }

    /** อ่าน token จาก cookie ถ้าไม่มีหรือว่างจะได้ Optional ว่าง */
    private Optional<String> readToken(ServerHttpRequest request) {
        return Optional.ofNullable(request.getCookies().getFirst(COOKIE_NAME))
                .map(HttpCookie::getValue)
                .filter(value -> !value.isBlank());
    }

    /** ตรวจลายเซ็นและวันหมดอายุ ถ้าไม่ผ่านจะได้ Optional ว่าง */
    private Optional<Claims> verify(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ServerHttpRequest withUserHeaders(ServerHttpRequest request, Claims claims) {
        return request.mutate()
                .header(HEADER_USER_ID, claims.getSubject())
                .header(HEADER_USER_TYPE, claims.get("typ", String.class))
                .build();
    }

    // ---------- ส่งต่อ / ปฏิเสธ ----------

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain,
                               ServerHttpRequest request) {
        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        byte[] body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"กรุณาเข้าสู่ระบบ\"}"
                .getBytes(StandardCharsets.UTF_8);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    /** ทำงานก่อน filter อื่นทั้งหมด */
    @Override
    public int getOrder() {
        return -1;
    }
}