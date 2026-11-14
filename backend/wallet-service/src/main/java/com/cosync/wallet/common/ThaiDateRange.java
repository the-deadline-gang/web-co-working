package com.cosync.wallet.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;

/** แปลงช่วงวันที่ (ตามเวลาไทย) ที่ admin เลือก เป็นช่วงเวลา UTC สำหรับ query */
public final class ThaiDateRange {

    public static final ZoneId TH = ZoneId.of("Asia/Bangkok");
    private static final int MAX_DAYS = 366;

    private ThaiDateRange() {}         

    /** เที่ยงคืนต้นวัน (เวลาไทย) เช่น 2026-11-01 > 2026-10-31T17:00:00Z */
    public static Instant startOf(LocalDate day) {
        return day.atStartOfDay(TH).toInstant();
    }

    /** เที่ยงคืนของวันถัดไป (ใช้กับ < ไม่ใช่ <=) จะได้ทั้งวันของ day ครบ */
    public static Instant endOf(LocalDate day) {
        return day.plusDays(1).atStartOfDay(TH).toInstant();
    }

    /** from ต้องไม่อยู่หลัง to และห่างกันไม่เกิน 366 วัน */
    public static void validate(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "from ต้องไม่อยู่หลัง to");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "ดูได้ไม่เกิน " + MAX_DAYS + " วันต่อครั้ง");
        }
    }
}