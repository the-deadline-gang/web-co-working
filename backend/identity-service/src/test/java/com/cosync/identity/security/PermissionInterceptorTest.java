package com.cosync.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.cosync.identity.common.ApiException;

class PermissionInterceptorTest {

    private final PermissionInterceptor interceptor = new PermissionInterceptor();

    /** controller ปลอม มีเมธอดที่แปะป้ายแต่ละแบบ */
    static class SampleController {
        public void open() { }

        @MemberOnly
        public void memberOnly() { }

        @StaffOnly
        public void staffOnly() { }

        @RequirePermission("role.manage")
        public void manageRoles() { }
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new SampleController(), SampleController.class.getMethod(methodName));
    }

    /** request ที่มี header เหมือนที่ Gateway ใส่มา (null = ไม่มี header นั้น) */
    private static MockHttpServletRequest request(String userType, String permissions) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userType != null) {
            request.addHeader("X-User-Type", userType);
        }
        if (permissions != null) {
            request.addHeader("X-Permissions", permissions);
        }
        return request;
    }

    private void assertAllowed(String method, String userType, String permissions) throws Exception {
        assertTrue(interceptor.preHandle(request(userType, permissions), new MockHttpServletResponse(), handler(method)));
    }

    private void assertForbidden(String method, String userType, String permissions) throws Exception {
        HandlerMethod handler = handler(method);
        ApiException e = assertThrows(ApiException.class, () ->
                interceptor.preHandle(request(userType, permissions), new MockHttpServletResponse(), handler));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertEquals("FORBIDDEN", e.getCode());
    }

    // ---------- ไม่มีป้าย ----------

    @Test
    @DisplayName("ไม่มีป้าย → ผ่านเสมอ")
    void noAnnotation_allowed() throws Exception {
        assertAllowed("open", null, null);
        assertAllowed("open", "MEMBER", null);
    }

    @Test
    @DisplayName("TC-SEC-10 handler ไม่ใช่เมธอดของ controller (path ที่ไม่มีอยู่) → ปล่อยให้ไปจบที่ 404")
    void notHandlerMethod_allowed() {
        assertTrue(interceptor.preHandle(request(null, null), new MockHttpServletResponse(), new Object()));
    }

    // ---------- @MemberOnly ----------

    @Test
    @DisplayName("TC-ME-01 @MemberOnly + MEMBER → ผ่าน")
    void memberOnly_member_allowed() throws Exception {
        assertAllowed("memberOnly", "MEMBER", null);
    }

    @Test
    @DisplayName("TC-SEC-09 @MemberOnly + STAFF → 403")
    void memberOnly_staff_forbidden() throws Exception {
        assertForbidden("memberOnly", "STAFF", "role.manage");
    }

    @Test
    @DisplayName("@MemberOnly ไม่มี X-User-Type → 403")
    void memberOnly_noType_forbidden() throws Exception {
        assertForbidden("memberOnly", null, null);
    }

    // ---------- @StaffOnly ----------

    @Test
    @DisplayName("@StaffOnly + STAFF ไม่มีสิทธิ์เลย → ผ่าน (ไม่สนสิทธิ์)")
    void staffOnly_staff_allowed() throws Exception {
        assertAllowed("staffOnly", "STAFF", null);
    }

    @Test
    @DisplayName("TC-PWD-05 @StaffOnly + MEMBER → 403")
    void staffOnly_member_forbidden() throws Exception {
        assertForbidden("staffOnly", "MEMBER", null);
    }

    // ---------- @RequirePermission ----------

    @Test
    @DisplayName("@RequirePermission + STAFF มีสิทธิ์ → ผ่าน")
    void requirePermission_hasPermission_allowed() throws Exception {
        assertAllowed("manageRoles", "STAFF", "employee.manage,role.manage");
    }

    @Test
    @DisplayName("TC-SEC-06 STAFF ไม่มีสิทธิ์ที่ต้องการ (Counter) → 403")
    void requirePermission_missingPermission_forbidden() throws Exception {
        assertForbidden("manageRoles", "STAFF", "loan.record,checkout.override");
    }

    @Test
    @DisplayName("TC-SEC-08 MEMBER อ้างสิทธิ์มาใน header → 403")
    void requirePermission_member_forbidden() throws Exception {
        assertForbidden("manageRoles", "MEMBER", "role.manage");
    }

    @Test
    @DisplayName("STAFF ไม่มี header X-Permissions → 403")
    void requirePermission_noHeader_forbidden() throws Exception {
        assertForbidden("manageRoles", "STAFF", null);
    }

    @Test
    @DisplayName("สิทธิ์ต้องตรงทั้งคำ: role.manage.extra ไม่นับเป็น role.manage")
    void requirePermission_exactMatchOnly() throws Exception {
        assertForbidden("manageRoles", "STAFF", "role.manage.extra,xrole.manage");
    }
}
