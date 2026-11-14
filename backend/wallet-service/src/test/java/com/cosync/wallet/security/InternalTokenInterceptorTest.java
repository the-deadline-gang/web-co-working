package com.cosync.wallet.security;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalTokenInterceptorTest {

    private final InternalTokenInterceptor interceptor = new InternalTokenInterceptor("internal-secret");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/internal/wallets");
        if (token != null) {
            r.addHeader("X-Internal-Token", token);
        }
        return r;
    }

    @Test
    @DisplayName("TC-ITK-01 token ถูก → ผ่าน")
    void validToken() {
        assertTrue(interceptor.preHandle(request("internal-secret"), response, new Object()));
    }

    @Test
    @DisplayName("TC-ITK-02 ไม่ส่ง / ผิด / ว่าง → 401 INVALID_INTERNAL_TOKEN")
    void invalidToken() {
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_TOKEN",
                () -> interceptor.preHandle(request(null), response, new Object()));
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_TOKEN",
                () -> interceptor.preHandle(request("wrong"), response, new Object()));
        assertApiError(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_TOKEN",
                () -> interceptor.preHandle(request(""), response, new Object()));
    }
}
