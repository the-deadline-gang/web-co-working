package com.cosync.booking.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cosync.booking.entity.enumtype.LoanResult;
import com.cosync.booking.entity.enumtype.LoanStatus;
import com.cosync.booking.entity.enumtype.ServiceType;

/**
 * Loan: ยืม (F8) คืน (F9) ค้างคืน/หาย (BR-33) แก้ผลย้อนหลัง (ส่วน 2.4)
 * ตัวเลขใช้สาย HDMI ค่ายืม 20 ประกัน 300 ตามตัวอย่างใน spec (TC-10, TC-11)
 */
class LoanTest {

    private static final Instant NOW = Instant.parse("2026-11-20T07:00:00Z");
    private static final BigDecimal FEE = new BigDecimal("20.00");
    private static final BigDecimal DEPOSIT = new BigDecimal("300.00");
    private static final BigDecimal HALF = new BigDecimal("150.00");

    private EquipmentType hdmi;
    private Equipment cable;

    @BeforeEach
    void setUp() {
        hdmi = new EquipmentType("สาย HDMI", true);
        hdmi.setPricing(FEE, DEPOSIT, "STF-002");
        cable = new Equipment(hdmi, "EQ-L-001", null);
    }

    private Loan borrowed() {
        return Loan.borrow(5L, cable, 9, "STF-005", NOW);
    }

    private Loan returned(LoanResult result, BigDecimal refund) {
        Loan loan = borrowed();
        loan.markReturned(8, result, refund, "STF-005", null, NOW.plusSeconds(3600));
        return loan;
    }

    // ------------------------------------------------------------ borrow

    @Nested
    @DisplayName("borrow(): ยืมอุปกรณ์")
    class Borrow {

        @Test
        @DisplayName("TC-LNE-01 ได้ BORROWED และคัดลอกค่ายืม + ประกันจากประเภทตอนยืม")
        void createsBorrowed() {
            Loan loan = borrowed();

            assertEquals(LoanStatus.BORROWED, loan.getStatus());
            assertEquals(5L, loan.getUserId());
            assertSame(cable, loan.getEquipment());
            assertEquals(9, loan.getConditionBefore());
            assertEquals(FEE, loan.getFee());
            assertEquals(DEPOSIT, loan.getDeposit());
            assertEquals("STF-005", loan.getLentBy());
            assertEquals(NOW, loan.getBorrowedAt());
            assertNull(loan.getResult());
            assertNull(loan.getDepositRefund());
        }

        @Test
        @DisplayName("TC-LNE-02 การเงินเปลี่ยนราคาหลังยืม รายการเดิมยังเป็นราคาตอนยืม")
        void priceIsCopied() {
            Loan loan = borrowed();

            hdmi.setPricing(new BigDecimal("50.00"), new BigDecimal("500.00"), "STF-002");

            assertEquals(FEE, loan.getFee());
            assertEquals(DEPOSIT, loan.getDeposit());
        }

        @Test
        @DisplayName("TC-LNE-03 ของประจำห้อง (มี resource) → IllegalArgumentException")
        void fixedEquipment() {
            Resource room = Resource.bookable(new Zone("B"), "B-ROOM-01", ServiceType.MEETING, 4, null, null);
            Equipment tv = new Equipment(hdmi, "EQ-F-001", room);

            assertThrows(IllegalArgumentException.class, () -> Loan.borrow(5L, tv, 9, "STF-005", NOW));
        }

        @Test
        @DisplayName("TC-LNE-04 ประเภทที่ยังไม่ตั้งราคา → IllegalArgumentException")
        void noPricing() {
            Equipment noPrice = new Equipment(new EquipmentType("ไมค์ใหม่", true), "EQ-L-900", null);

            assertThrows(IllegalArgumentException.class, () -> Loan.borrow(5L, noPrice, 9, "STF-005", NOW));
        }

        @Test
        @DisplayName("TC-LNE-05 คะแนนตอนยืม 0 หรือ 11 → IllegalArgumentException ส่วน 1 และ 10 ใช้ได้")
        void conditionRange() {
            assertThrows(IllegalArgumentException.class, () -> Loan.borrow(5L, cable, 0, "STF-005", NOW));
            assertThrows(IllegalArgumentException.class, () -> Loan.borrow(5L, cable, 11, "STF-005", NOW));
            assertEquals(1, Loan.borrow(5L, cable, 1, "STF-005", NOW).getConditionBefore());
            assertEquals(10, Loan.borrow(5L, cable, 10, "STF-005", NOW).getConditionBefore());
        }
    }

    // ------------------------------------------------------------ markReturned

    @Nested
    @DisplayName("markReturned(): คืนอุปกรณ์")
    class Return {

        @Test
        @DisplayName("TC-LNE-06 คืนปกติ → RETURNED พร้อมบันทึกผลครบ (TC-11 คะแนน 9 → 8 คืน 100%)")
        void returnNormal() {
            Loan loan = borrowed();
            Instant at = NOW.plusSeconds(3600);

            loan.markReturned(8, LoanResult.NORMAL, DEPOSIT, "STF-007", "https://img/1.jpg", at);

            assertEquals(LoanStatus.RETURNED, loan.getStatus());
            assertEquals(8, loan.getConditionAfter());
            assertEquals(LoanResult.NORMAL, loan.getResult());
            assertEquals(DEPOSIT, loan.getDepositRefund());
            assertEquals("STF-007", loan.getReceivedBy());
            assertEquals("https://img/1.jpg", loan.getPhotoUrl());
            assertEquals(at, loan.getReturnedAt());
        }

        @Test
        @DisplayName("TC-LNE-07 refund 0 (เสียหายหนัก) และคืนเต็มประกันทำได้")
        void refundBoundaries() {
            assertEquals(0, returned(LoanResult.MAJOR, BigDecimal.ZERO).getDepositRefund().signum());
            assertEquals(DEPOSIT, returned(LoanResult.NORMAL, DEPOSIT).getDepositRefund());
        }

        @Test
        @DisplayName("TC-LNE-08 refund ติดลบหรือเกินเงินประกัน → IllegalArgumentException และยัง BORROWED")
        void refundOutOfRange() {
            Loan loan = borrowed();

            assertThrows(IllegalArgumentException.class,
                    () -> loan.markReturned(8, LoanResult.NORMAL, new BigDecimal("-1"), "STF-005", null, NOW));
            assertThrows(IllegalArgumentException.class,
                    () -> loan.markReturned(8, LoanResult.NORMAL, new BigDecimal("300.01"), "STF-005", null, NOW));
            assertEquals(LoanStatus.BORROWED, loan.getStatus());
        }

        @Test
        @DisplayName("TC-LNE-09 คะแนนตอนคืน 0 หรือ 11 → IllegalArgumentException")
        void conditionAfterRange() {
            Loan loan = borrowed();

            assertThrows(IllegalArgumentException.class,
                    () -> loan.markReturned(0, LoanResult.MAJOR, BigDecimal.ZERO, "STF-005", null, NOW));
            assertThrows(IllegalArgumentException.class,
                    () -> loan.markReturned(11, LoanResult.NORMAL, DEPOSIT, "STF-005", null, NOW));
            assertEquals(LoanStatus.BORROWED, loan.getStatus());
        }

        @Test
        @DisplayName("TC-LNE-10 คืนตอน OVERDUE ได้ (นำมาคืนทีหลัง คืนประกันตามสภาพ)")
        void returnWhenOverdue() {
            Loan loan = borrowed();
            loan.markOverdue(NOW.plusSeconds(3600));

            loan.markReturned(7, LoanResult.MINOR, HALF, "STF-005", null, NOW.plusSeconds(86_400));

            assertEquals(LoanStatus.RETURNED, loan.getStatus());
            assertEquals(HALF, loan.getDepositRefund());
        }

        @Test
        @DisplayName("TC-LNE-11 คืนซ้ำ หรือคืนของที่ LOST แล้ว → IllegalStateException")
        void cannotReturnTwiceOrLost() {
            Loan loan = returned(LoanResult.NORMAL, DEPOSIT);
            assertThrows(IllegalStateException.class,
                    () -> loan.markReturned(8, LoanResult.NORMAL, DEPOSIT, "STF-005", null, NOW));

            Loan lost = borrowed();
            lost.markOverdue(NOW);
            lost.markLost();
            assertThrows(IllegalStateException.class,
                    () -> lost.markReturned(8, LoanResult.NORMAL, DEPOSIT, "STF-005", null, NOW));
        }
    }

    // ------------------------------------------------------------ overdue / lost

    @Nested
    @DisplayName("markOverdue() / markLost(): ค้างคืนและหาย (BR-33)")
    class OverdueLost {

        @Test
        @DisplayName("TC-LNE-12 BORROWED → OVERDUE บันทึก overdueAt")
        void overdue() {
            Loan loan = borrowed();
            Instant closing = NOW.plusSeconds(8 * 3600);

            loan.markOverdue(closing);

            assertEquals(LoanStatus.OVERDUE, loan.getStatus());
            assertEquals(closing, loan.getOverdueAt());
        }

        @Test
        @DisplayName("TC-LNE-13 OVERDUE → LOST ผล LOST และไม่คืนเงินประกัน")
        void lost() {
            Loan loan = borrowed();
            loan.markOverdue(NOW);

            loan.markLost();

            assertEquals(LoanStatus.LOST, loan.getStatus());
            assertEquals(LoanResult.LOST, loan.getResult());
            assertEquals(0, loan.getDepositRefund().signum());
        }

        @Test
        @DisplayName("TC-LNE-14 เส้นทางที่ไม่อนุญาต: LOST จาก BORROWED, OVERDUE ซ้ำ, OVERDUE หลังคืนแล้ว")
        void notAllowed() {
            Loan borrowed = borrowed();
            assertThrows(IllegalStateException.class, borrowed::markLost);

            Loan overdue = borrowed();
            overdue.markOverdue(NOW);
            assertThrows(IllegalStateException.class, () -> overdue.markOverdue(NOW));

            Loan done = returned(LoanResult.NORMAL, DEPOSIT);
            assertThrows(IllegalStateException.class, () -> done.markOverdue(NOW));
        }
    }

    // ------------------------------------------------------------ adjust

    @Nested
    @DisplayName("adjust(): ผู้จัดการแก้ผลย้อนหลัง")
    class Adjust {

        @Test
        @DisplayName("TC-LNE-15 MINOR → NORMAL คืนเพิ่มได้ บันทึกผู้แก้ เวลา เหตุผล")
        void adjustHigher() {
            Loan loan = returned(LoanResult.MINOR, HALF);
            Instant at = NOW.plusSeconds(86_400);

            loan.adjust(LoanResult.NORMAL, DEPOSIT, "STF-001", "ดูรูปแล้วรอยมีอยู่ก่อนยืม", at);

            assertEquals(LoanResult.NORMAL, loan.getResult());
            assertEquals(DEPOSIT, loan.getDepositRefund());
            assertEquals("STF-001", loan.getAdjustedBy());
            assertEquals(at, loan.getAdjustedAt());
            assertEquals("ดูรูปแล้วรอยมีอยู่ก่อนยืม", loan.getAdjustReason());
            assertEquals(LoanStatus.RETURNED, loan.getStatus());
        }

        @Test
        @DisplayName("TC-LNE-16 ลดเงินคืนย้อนหลังไม่ได้ (RESULT_NOT_HIGHER) และเท่าเดิมก็ไม่ได้")
        void notHigher() {
            Loan loan = returned(LoanResult.NORMAL, DEPOSIT);
            assertThrows(IllegalArgumentException.class,
                    () -> loan.adjust(LoanResult.MINOR, HALF, "STF-001", "ลด", NOW));

            Loan same = returned(LoanResult.MINOR, HALF);
            assertThrows(IllegalArgumentException.class,
                    () -> same.adjust(LoanResult.MINOR, HALF, "STF-001", "เท่าเดิม", NOW));
            assertEquals(HALF, same.getDepositRefund());
        }

        @Test
        @DisplayName("TC-LNE-17 คืนเกินเงินประกัน → IllegalArgumentException")
        void overDeposit() {
            Loan loan = returned(LoanResult.MINOR, HALF);

            assertThrows(IllegalArgumentException.class,
                    () -> loan.adjust(LoanResult.NORMAL, new BigDecimal("300.01"), "STF-001", "เกิน", NOW));
        }

        @Test
        @DisplayName("TC-LNE-18 แก้ได้ครั้งเดียว ครั้งที่ 2 → IllegalStateException")
        void onlyOnce() {
            Loan loan = returned(LoanResult.MAJOR, BigDecimal.ZERO);
            loan.adjust(LoanResult.MINOR, HALF, "STF-001", "ครั้งแรก", NOW);

            assertThrows(IllegalStateException.class,
                    () -> loan.adjust(LoanResult.NORMAL, DEPOSIT, "STF-001", "ครั้งที่ 2", NOW));
            assertEquals(HALF, loan.getDepositRefund());
        }

        @Test
        @DisplayName("TC-LNE-19 แก้รายการที่ยังไม่คืน → IllegalStateException")
        void notReturnedYet() {
            Loan loan = borrowed();

            assertThrows(IllegalStateException.class,
                    () -> loan.adjust(LoanResult.NORMAL, DEPOSIT, "STF-001", "ยังไม่คืน", NOW));
        }
    }
}
