package com.cosync.booking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.cosync.booking.security.MemberOnly;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.security.StaffOnly;
import com.cosync.booking.service.RateService;
import com.cosync.booking.service.SettingService;
import com.cosync.booking.service.ZoneService;

/**
 * กันลืมใส่สิทธิ์: PermissionInterceptor เป็นแบบ fail-open (ไม่มี annotation = ใครก็ผ่าน)
 * test นี้ไล่ทุก endpoint ของ booking แล้วแดงทันทีถ้ามีตัวไหนไม่มี annotation
 *
 * @WebMvcTest ไม่ระบุคลาส = โหลด controller ทุกตัวในโปรเจกต์ ตัวใหม่จึงถูกตรวจอัตโนมัติ
 * พอเพิ่ม controller ที่ใช้ service ใหม่ test จะพังด้วย "No qualifying bean of type XxxService"
 * ให้เพิ่ม @MockitoBean XxxService ด้านล่าง
 */
@WebMvcTest
class EndpointSecurityTest {

    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping;

    @MockitoBean SettingService settingService;
    @MockitoBean RateService rateService;
    @MockitoBean ZoneService zoneService;

    /** endpoint ที่ "ตั้งใจ" ไม่มี annotation สิทธิ์ พร้อมเหตุผล (ตอนนี้ยังไม่มี จะมี /internal/** ใน B-04) */
    private static final Set<String> NO_ANNOTATION_ALLOWED = Set.of();

    @Test
    @DisplayName("TC-SEC-01 ทุก endpoint ต้องมี @MemberOnly / @StaffOnly / @RequirePermission ยกเว้นที่อยู่ใน allowlist")
    void everyEndpointIsGuarded() {
        List<String> unguarded = new ArrayList<>();
        Set<String> allPaths = new HashSet<>();

        mapping.getHandlerMethods().forEach((info, method) -> {
            if (!method.getBeanType().getPackageName().startsWith("com.cosync.booking")) {
                return;                                     // ข้าม controller ของ Spring เอง (เช่น /error)
            }
            Set<String> paths = info.getPatternValues();
            allPaths.addAll(paths);
            boolean guarded = method.hasMethodAnnotation(MemberOnly.class)
                    || method.hasMethodAnnotation(StaffOnly.class)
                    || method.hasMethodAnnotation(RequirePermission.class);
            if (!guarded && !NO_ANNOTATION_ALLOWED.containsAll(paths)) {
                unguarded.add(info + " → " + method.getShortLogMessage());
            }
        });

        assertEquals(List.of(), unguarded,
                "endpoint เหล่านี้ไม่มี annotation สิทธิ์ ใส่ให้ถูก หรือเพิ่มใน NO_ANNOTATION_ALLOWED พร้อมเหตุผล");
        assertTrue(allPaths.containsAll(NO_ANNOTATION_ALLOWED),
                "allowlist มี path ที่ไม่มีอยู่จริงแล้ว ลบออกจาก NO_ANNOTATION_ALLOWED: " + NO_ANNOTATION_ALLOWED);
    }

    @Test
    @DisplayName("TC-SEC-02 test นี้เห็น endpoint จริง (กัน TC-SEC-01 ผ่านเพราะไม่ได้ตรวจอะไรเลย)")
    void seesAllControllers() {
        Set<String> paths = new HashSet<>();
        mapping.getHandlerMethods().forEach((info, m) -> paths.addAll(info.getPatternValues()));
        assertTrue(paths.containsAll(Set.of("/admin/settings", "/admin/rates", "/admin/rates/{serviceType}",
                "/admin/zones")),
                "ไม่เห็นบาง endpoint: " + paths);
    }
}
