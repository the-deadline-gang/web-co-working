package com.cosync.wallet.security;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.cosync.wallet.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ทำงานก่อนเข้า Controller ทุกครั้ง เช็ก @MemberOnly, @StaffOnly และ @RequirePermission
 * เชื่อ X-User-Type / X-Permissions ได้เพราะ Gateway ลบค่าที่ client ส่งมาทิ้ง แล้วใส่ใหม่จาก JWT
 * เป็นแบบ fail-open: method ที่ไม่มี annotation จะผ่านเสมอ
 * EndpointSecurityTest คอยเช็กว่าทุก endpoint มี annotation (ยกเว้น webhook)
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        String userType = request.getHeader("X-User-Type");

        // สมาชิกเท่านั้น
        if (method.hasMethodAnnotation(MemberOnly.class) && !"MEMBER".equals(userType)) {
            throw forbidden();
        }

        // พนักงานเท่านั้น (ไม่สนว่ามีสิทธิ์อะไร)
        if (method.hasMethodAnnotation(StaffOnly.class) && !"STAFF".equals(userType)) {
            throw forbidden();
        }

        // พนักงานที่มีสิทธิ์เฉพาะ
        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required != null) {
            String perms = request.getHeader("X-Permissions");
            boolean hasPermission = perms != null
                    && Arrays.asList(perms.split(",")).contains(required.value());
            if (!"STAFF".equals(userType) || !hasPermission) {
                throw forbidden();
            }
        }

        return true;
    }

    private ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ไม่มีสิทธิ์เข้าถึง");
    }
}
