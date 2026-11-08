package com.cosync.identity.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** ทดสอบ handler ตรงๆ สำหรับกรณีที่ยิงผ่าน HTTP ให้เกิดได้ยาก (ชน UNIQUE ใน DB, บั๊กที่ไม่คาดไว้) */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** จำลอง error ของ MySQL ตอนชน UNIQUE */
    private static DataIntegrityViolationException duplicate(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Duplicate entry 'x' for key '" + constraint + "'"));
    }

    private void assertDuplicateMapsTo(String constraint, String expectedCode) {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(duplicate(constraint));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(expectedCode, response.getBody().code());
    }

    @Test
    @DisplayName("TC-REG-02 สมัครพร้อมกันจนชน uk_app_user_email → 409 EMAIL_TAKEN")
    void duplicateUserEmail() {
        assertDuplicateMapsTo("app_user.uk_app_user_email", "EMAIL_TAKEN");
    }

    @Test
    @DisplayName("TC-REG-04 ชน uk_app_user_phone → 409 PHONE_TAKEN")
    void duplicateUserPhone() {
        assertDuplicateMapsTo("app_user.uk_app_user_phone", "PHONE_TAKEN");
    }

    @Test
    @DisplayName("TC-EMP-03 สร้างพนักงานพร้อมกันจนชน uk_employee_email → 409 EMAIL_TAKEN")
    void duplicateEmployeeEmail() {
        assertDuplicateMapsTo("employee.uk_employee_email", "EMAIL_TAKEN");
    }

    @Test
    @DisplayName("TC-ROL-04 สร้าง role พร้อมกันจนชน uk_role_name → 409 ROLE_NAME_TAKEN")
    void duplicateRoleName() {
        assertDuplicateMapsTo("role.uk_role_name", "ROLE_NAME_TAKEN");
    }

    @Test
    @DisplayName("ชน constraint ที่ไม่รู้จัก → 409 CONFLICT")
    void unknownConstraint() {
        assertDuplicateMapsTo("some_table.uk_something_else", "CONFLICT");
    }

    @Test
    @DisplayName("บั๊กที่ไม่คาดไว้ → 500 INTERNAL_ERROR โดยไม่บอกรายละเอียดภายใน")
    void unexpected_hidesDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("password_hash column missing at row 42"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertNull(response.getBody().details());
        assertEquals(false, response.getBody().message().contains("password_hash"));
    }
}
