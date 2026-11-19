package com.cosync.booking.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cosync.booking.entity.enumtype.BookingStatus;
import com.cosync.booking.entity.enumtype.ServiceType;

/**
 * state machine ของ Booking (System Flow ส่วนที่ 4.1)
 * ทุกลูกศรที่อนุญาตต้องผ่าน และทุกลูกศรที่ไม่อนุญาตต้อง throw
 * ไม่มี Spring ไม่มี DB เป็น unit test ล้วน
 */
class BookingTest {

    // 20 พ.ย. 13:50 เวลาไทย = 06:50 UTC (จองห้อง 14:00–16:00)
    private static final Instant NOW = Instant.parse("2026-11-20T06:50:00Z");
    private static final Instant START = Instant.parse("2026-11-20T07:00:00Z");
    private static final Instant END = Instant.parse("2026-11-20T09:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("240.00");

    private final Resource room = Resource.bookable(new Zone("B"), "B-ROOM-02", ServiceType.MEETING, 6, null, null);

    private Booking pending() {
        return Booking.hold(4L, room, START, END, PRICE, NOW, 10);
    }

    private Booking confirmed() {
        Booking b = pending();
        b.confirm(NOW.plusSeconds(60));
        return b;
    }

    private Booking checkedIn() {
        Booking b = confirmed();
        b.checkIn(START.minusSeconds(300));
        return b;
    }

    // ------------------------------------------------------------ hold

    @Nested
    @DisplayName("hold(): สร้างการจองใหม่")
    class Hold {

        @Test
        @DisplayName("TC-BKE-01 ได้ PENDING_PAYMENT, ล็อก 10 นาทีจาก now, คัดลอกประเภทจาก resource")
        void createsPending() {
            Booking b = pending();

            assertEquals(BookingStatus.PENDING_PAYMENT, b.getStatus());
            assertEquals(4L, b.getUserId());
            assertSame(room, b.getResource());
            assertEquals(ServiceType.MEETING, b.getServiceType());
            assertEquals(START, b.getStartAt());
            assertEquals(END, b.getEndAt());
            assertEquals(PRICE, b.getPrice());
            assertEquals(Instant.parse("2026-11-20T07:00:00Z"), b.getHoldUntil());
            assertFalse(b.isHoldExtended());
            assertNull(b.getPaidAt());
            assertNull(b.getRefundAmount());
        }

        @Test
        @DisplayName("TC-BKE-02 endAt เท่ากับหรือก่อน startAt → IllegalArgumentException")
        void rejectsBadTimeRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> Booking.hold(4L, room, START, START, PRICE, NOW, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> Booking.hold(4L, room, END, START, PRICE, NOW, 10));
        }

        @Test
        @DisplayName("TC-BKE-03 ราคาติดลบ → IllegalArgumentException แต่ราคา 0 (ส่วนลด 100%) สร้างได้")
        void price() {
            assertThrows(IllegalArgumentException.class,
                    () -> Booking.hold(4L, room, START, END, new BigDecimal("-0.01"), NOW, 10));
            assertEquals(BigDecimal.ZERO, Booking.hold(4L, room, START, END, BigDecimal.ZERO, NOW, 10).getPrice());
        }
    }

    // ------------------------------------------------------------ PENDING_PAYMENT →

    @Nested
    @DisplayName("จาก PENDING_PAYMENT")
    class FromPending {

        @Test
        @DisplayName("TC-BKE-04 confirm → CONFIRMED, บันทึก paidAt, ล้าง holdUntil")
        void confirm() {
            Booking b = pending();
            Instant paidAt = NOW.plusSeconds(60);

            b.confirm(paidAt);

            assertEquals(BookingStatus.CONFIRMED, b.getStatus());
            assertEquals(paidAt, b.getPaidAt());
            assertNull(b.getHoldUntil());
        }

        @Test
        @DisplayName("TC-BKE-05 expire → EXPIRED, ล้าง holdUntil")
        void expire() {
            Booking b = pending();

            b.expire();

            assertEquals(BookingStatus.EXPIRED, b.getStatus());
            assertNull(b.getHoldUntil());
        }

        @Test
        @DisplayName("TC-BKE-06 extendHold ครั้งแรก: ต่อจาก holdUntil เดิม 10 + 15 = 25 นาที (TC-05)")
        void extendOnce() {
            Booking b = pending();

            b.extendHold(15);

            assertEquals(Instant.parse("2026-11-20T07:15:00Z"), b.getHoldUntil());
            assertTrue(b.isHoldExtended());
            assertEquals(BookingStatus.PENDING_PAYMENT, b.getStatus());
        }

        @Test
        @DisplayName("TC-BKE-07 extendHold ครั้งที่ 2 → IllegalStateException และ holdUntil ไม่เปลี่ยน (BR-17)")
        void extendTwice() {
            Booking b = pending();
            b.extendHold(15);

            assertThrows(IllegalStateException.class, () -> b.extendHold(15));
            assertEquals(Instant.parse("2026-11-20T07:15:00Z"), b.getHoldUntil());
        }

        @Test
        @DisplayName("TC-BKE-08 PENDING_PAYMENT ทำ cancel, checkIn, markNoShow, complete ไม่ได้")
        void notAllowed() {
            Booking b = pending();

            assertThrows(IllegalStateException.class, () -> b.cancel(NOW, BigDecimal.ZERO));
            assertThrows(IllegalStateException.class, () -> b.checkIn(NOW));
            assertThrows(IllegalStateException.class, b::markNoShow);
            assertThrows(IllegalStateException.class, b::complete);
            assertEquals(BookingStatus.PENDING_PAYMENT, b.getStatus());
        }
    }

    // ------------------------------------------------------------ CONFIRMED →

    @Nested
    @DisplayName("จาก CONFIRMED")
    class FromConfirmed {

        @Test
        @DisplayName("TC-BKE-09 cancel → CANCELLED พร้อม cancelledAt และ refundAmount")
        void cancel() {
            Booking b = confirmed();

            b.cancel(NOW, new BigDecimal("120.00"));

            assertEquals(BookingStatus.CANCELLED, b.getStatus());
            assertEquals(NOW, b.getCancelledAt());
            assertEquals(new BigDecimal("120.00"), b.getRefundAmount());
        }

        @Test
        @DisplayName("TC-BKE-10 cancel คืน 0 ได้ (ยกเลิกก่อนเริ่มไม่ถึง 2 ชม.) และคืนเต็มราคาได้")
        void cancelBoundaries() {
            Booking zero = confirmed();
            zero.cancel(NOW, BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, zero.getRefundAmount());

            Booking full = confirmed();
            full.cancel(NOW, PRICE);
            assertEquals(PRICE, full.getRefundAmount());
        }

        @Test
        @DisplayName("TC-BKE-11 refund ติดลบหรือเกินราคา → IllegalArgumentException และยังเป็น CONFIRMED")
        void cancelBadRefund() {
            Booking b = confirmed();

            assertThrows(IllegalArgumentException.class, () -> b.cancel(NOW, new BigDecimal("-1")));
            assertThrows(IllegalArgumentException.class, () -> b.cancel(NOW, new BigDecimal("240.01")));
            assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        }

        @Test
        @DisplayName("TC-BKE-12 checkIn → CHECKED_IN และบันทึก checkedInAt")
        void checkIn() {
            Booking b = confirmed();
            Instant at = START.minusSeconds(300);

            b.checkIn(at);

            assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
            assertEquals(at, b.getCheckedInAt());
        }

        @Test
        @DisplayName("TC-BKE-13 markNoShow → NO_SHOW")
        void noShow() {
            Booking b = confirmed();

            b.markNoShow();

            assertEquals(BookingStatus.NO_SHOW, b.getStatus());
        }

        @Test
        @DisplayName("TC-BKE-14 CONFIRMED ทำ confirm ซ้ำ, expire, extendHold, complete ไม่ได้")
        void notAllowed() {
            Booking b = confirmed();

            assertThrows(IllegalStateException.class, () -> b.confirm(NOW));
            assertThrows(IllegalStateException.class, b::expire);
            assertThrows(IllegalStateException.class, () -> b.extendHold(15));
            assertThrows(IllegalStateException.class, b::complete);
            assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        }
    }

    // ------------------------------------------------------------ CHECKED_IN →

    @Nested
    @DisplayName("จาก CHECKED_IN")
    class FromCheckedIn {

        @Test
        @DisplayName("TC-BKE-15 complete → COMPLETED")
        void complete() {
            Booking b = checkedIn();

            b.complete();

            assertEquals(BookingStatus.COMPLETED, b.getStatus());
        }

        @Test
        @DisplayName("TC-BKE-16 คนเข้ามาแล้วถูก markNoShow ไม่ได้ (กัน job no-show ทับ) และยกเลิกไม่ได้")
        void cannotNoShowOrCancel() {
            Booking b = checkedIn();

            assertThrows(IllegalStateException.class, b::markNoShow);
            assertThrows(IllegalStateException.class, () -> b.cancel(NOW, BigDecimal.ZERO));
            assertThrows(IllegalStateException.class, () -> b.checkIn(NOW));
            assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        }
    }

    // ------------------------------------------------------------ สถานะสุดท้าย

    @Test
    @DisplayName("TC-BKE-17 EXPIRED, CANCELLED, NO_SHOW, COMPLETED เป็นสถานะสุดท้าย ไปต่อไม่ได้")
    void terminalStates() {
        Booking expired = pending();
        expired.expire();
        assertThrows(IllegalStateException.class, () -> expired.confirm(NOW));

        Booking cancelled = confirmed();
        cancelled.cancel(NOW, BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> cancelled.cancel(NOW, BigDecimal.ZERO));
        assertThrows(IllegalStateException.class, () -> cancelled.checkIn(NOW));

        Booking noShow = confirmed();
        noShow.markNoShow();
        assertThrows(IllegalStateException.class, () -> noShow.checkIn(NOW));

        Booking completed = checkedIn();
        completed.complete();
        assertThrows(IllegalStateException.class, completed::complete);
    }
}
