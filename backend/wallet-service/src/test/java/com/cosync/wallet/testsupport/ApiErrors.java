package com.cosync.wallet.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import com.cosync.wallet.common.ApiException;

public final class ApiErrors {

    private ApiErrors() {}

    /** เรียกแล้วต้องได้ ApiException ที่มี status และ code ตามที่คาด */
    public static ApiException assertApiError(HttpStatus status, String code, Executable call) {
        ApiException e = assertThrows(ApiException.class, call);
        assertEquals(status, e.getStatus(), "status");
        assertEquals(code, e.getCode(), "code");
        return e;
    }
}
