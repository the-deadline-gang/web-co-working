package com.cosync.wallet.common;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ThaiDateRangeTest {

    private static final LocalDate NOV_14 = LocalDate.of(2026, 11, 14);

    @Test
    @DisplayName("TC-TDR-01 เริ่มวันไทย = 17:00 UTC ของวันก่อน, จบวัน = เริ่มวันถัดไป")
    void boundaries() {
        assertEquals(Instant.parse("2026-11-13T17:00:00Z"), ThaiDateRange.startOf(NOV_14));
        assertEquals(Instant.parse("2026-11-14T17:00:00Z"), ThaiDateRange.endOf(NOV_14));
    }

    @Test
    @DisplayName("TC-TDR-02 วันเดียว และ 366 วัน (ปีอธิกสุรทิน) → ผ่าน")
    void validRanges() {
        assertDoesNotThrow(() -> ThaiDateRange.validate(NOV_14, NOV_14));
        assertDoesNotThrow(() -> ThaiDateRange.validate(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)));
    }

    @Test
    @DisplayName("TC-TDR-03 from หลัง to → 400")
    void fromAfterTo() {
        assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> ThaiDateRange.validate(NOV_14, NOV_14.minusDays(1)));
    }

    @Test
    @DisplayName("TC-TDR-04 เกิน 366 วัน → 400")
    void tooLong() {
        assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> ThaiDateRange.validate(NOV_14, NOV_14.plusDays(366)));
    }
}
