package com.cosync.wallet.security;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class PermissionInterceptorTest {

    /** controller ปลอมที่แปะ annotation แต่ละแบบ */
    static class Endpoints {
        @MemberOnly public void member() {}
        @StaffOnly public void staff() {}
        @RequirePermission("report.view") public void report() {}
        public void open() {}
    }

    private final PermissionInterceptor interceptor = new PermissionInterceptor();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private static HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new Endpoints(), method);
    }

    private static MockHttpServletRequest request(String userType, String permissions) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (userType != null) r.addHeader("X-User-Type", userType);
        if (permissions != null) r.addHeader("X-Permissions", permissions);
        return r;
    }

    private boolean allowed(String method, String userType, String permissions) throws Exception {
        return interceptor.preHandle(request(userType, permissions), response, handler(method));
    }

    private void assertForbidden(String method, String userType, String permissions) {
        assertApiError(HttpStatus.FORBIDDEN, "FORBIDDEN", () -> allowed(method, userType, permissions));
    }

    @Test
    @DisplayName("TC-PRM-01 @MemberOnly: MEMBER ผ่าน / STAFF และไม่มี header → 403")
    void memberOnly() throws Exception {
        assertTrue(allowed("member", "MEMBER", null));
        assertForbidden("member", "STAFF", null);
        assertForbidden("member", null, null);
    }

    @Test
    @DisplayName("TC-PRM-02 @StaffOnly: STAFF ผ่าน / MEMBER → 403")
    void staffOnly() throws Exception {
        assertTrue(allowed("staff", "STAFF", null));
        assertForbidden("staff", "MEMBER", null);
    }

    @Test
    @DisplayName("TC-PRM-03 @RequirePermission: STAFF ที่มีสิทธิ์ในรายการผ่าน")
    void permission_ok() throws Exception {
        assertTrue(allowed("report", "STAFF", "setting.manage,report.view"));
    }

    @Test
    @DisplayName("TC-PRM-04 @RequirePermission: ไม่มีสิทธิ์ / ชื่อคล้าย / MEMBER ที่ปลอม header → 403")
    void permission_denied() {
        assertForbidden("report", "STAFF", null);
        assertForbidden("report", "STAFF", "setting.manage");
        assertForbidden("report", "STAFF", "report.view.all");
        assertForbidden("report", "MEMBER", "report.view");
    }

    @Test
    @DisplayName("TC-PRM-05 endpoint ไม่มี annotation และ handler ที่ไม่ใช่ controller → ผ่าน")
    void openEndpoints() throws Exception {
        assertTrue(allowed("open", null, null));
        assertTrue(interceptor.preHandle(request(null, null), response, new Object()));
    }
}
