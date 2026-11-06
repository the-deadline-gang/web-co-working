package com.cosync.identity.security;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.cosync.identity.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** ทำงานก่อนเข้า Controller ทุกครั้ง เช็กสิทธิ์ถ้าเมธอดมี @RequirePermission */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ไม่ใช่เมธอดของ controller (เช่นไฟล์ static)  ข้าม
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // เมธอดนี้ไม่มีป้าย  ไม่ต้องเช็ก
        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            return true;
        }

        // ต้องเป็นพนักงาน และมีสิทธิ์ตรงกับที่ป้ายบอก
        boolean isStaff = "STAFF".equals(request.getHeader("X-User-Type"));
        String perms = request.getHeader("X-Permissions");
        boolean hasPermission = perms != null
                && Arrays.asList(perms.split(",")).contains(required.value());

        if (!isStaff || !hasPermission) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ไม่มีสิทธิ์เข้าถึง");
        }
        return true;
    }
}