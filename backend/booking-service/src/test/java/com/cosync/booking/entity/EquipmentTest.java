package com.cosync.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cosync.booking.entity.enumtype.EquipmentStatus;

/**
 * state machine ของ Equipment
 * AVAILABLE ⇄ MAINTENANCE (IT เปลี่ยนเอง) · AVAILABLE → BORROWED → AVAILABLE/MAINTENANCE/LOST
 */
class EquipmentTest {

    private Equipment newEquipment() {
        return new Equipment(new EquipmentType("สาย HDMI", true), "EQ-L-001", null);
    }

    private Equipment borrowed() {
        Equipment e = newEquipment();
        e.markBorrowed();
        return e;
    }

    private Equipment lost() {
        Equipment e = borrowed();
        e.markLost();
        return e;
    }

    @Test
    @DisplayName("TC-EQE-01 สร้างใหม่ต้องเป็น AVAILABLE")
    void newEquipmentIsAvailable() {
        assertThat(newEquipment().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-02 ยืม: AVAILABLE → BORROWED")
    void borrowFromAvailable() {
        assertThat(borrowed().getStatus()).isEqualTo(EquipmentStatus.BORROWED);
    }

    @Test
    @DisplayName("TC-EQE-03 ยืมซ้ำขณะถูกยืมอยู่ → IllegalStateException")
    void borrowTwiceFails() {
        Equipment e = borrowed();
        assertThatThrownBy(e::markBorrowed).isInstanceOf(IllegalStateException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.BORROWED);
    }

    @Test
    @DisplayName("TC-EQE-04 ยืมของที่กำลังซ่อม → IllegalStateException")
    void borrowMaintenanceFails() {
        Equipment e = newEquipment();
        e.changeStatus(EquipmentStatus.MAINTENANCE);
        assertThatThrownBy(e::markBorrowed).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-EQE-05 ยืมของที่หาย → IllegalStateException")
    void borrowLostFails() {
        assertThatThrownBy(lost()::markBorrowed).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-EQE-06 คืนสภาพดี → AVAILABLE")
    void returnGood() {
        Equipment e = borrowed();
        e.markReturned(false);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-07 คืนแบบต้องซ่อม → MAINTENANCE")
    void returnNeedsRepair() {
        Equipment e = borrowed();
        e.markReturned(true);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("TC-EQE-08 คืนของที่ไม่ได้ถูกยืม → IllegalStateException")
    void returnNotBorrowedFails() {
        Equipment e = newEquipment();
        assertThatThrownBy(() -> e.markReturned(false)).isInstanceOf(IllegalStateException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-09 คืนของที่บันทึกว่าหายไปแล้ว → IllegalStateException")
    void returnLostFails() {
        Equipment e = lost();
        assertThatThrownBy(() -> e.markReturned(false)).isInstanceOf(IllegalStateException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.LOST);
    }

    @Test
    @DisplayName("TC-EQE-10 หาย: BORROWED → LOST")
    void markLostFromBorrowed() {
        assertThat(lost().getStatus()).isEqualTo(EquipmentStatus.LOST);
    }

    @Test
    @DisplayName("TC-EQE-11 แจ้งหายของที่ไม่ได้ถูกยืม → IllegalStateException")
    void markLostFromAvailableFails() {
        assertThatThrownBy(newEquipment()::markLost).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TC-EQE-12 IT ส่งซ่อม AVAILABLE → MAINTENANCE แล้วกลับมา AVAILABLE ได้")
    void changeStatusRoundTrip() {
        Equipment e = newEquipment();
        e.changeStatus(EquipmentStatus.MAINTENANCE);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
        e.changeStatus(EquipmentStatus.AVAILABLE);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-13 เปลี่ยนเป็นค่าเดิม (AVAILABLE → AVAILABLE) ได้ ไม่ error")
    void changeStatusSameValue() {
        Equipment e = newEquipment();
        e.changeStatus(EquipmentStatus.AVAILABLE);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-14 IT ตั้งเป็น BORROWED เอง → IllegalArgumentException (400)")
    void changeStatusToBorrowedFails() {
        Equipment e = newEquipment();
        assertThatThrownBy(() -> e.changeStatus(EquipmentStatus.BORROWED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-15 IT ตั้งเป็น LOST เอง → IllegalArgumentException (400)")
    void changeStatusToLostFails() {
        Equipment e = newEquipment();
        assertThatThrownBy(() -> e.changeStatus(EquipmentStatus.LOST))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-16 เปลี่ยนสถานะของที่มีคนยืมอยู่ → IllegalStateException (409)")
    void changeStatusWhileBorrowedFails() {
        Equipment e = borrowed();
        assertThatThrownBy(() -> e.changeStatus(EquipmentStatus.MAINTENANCE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.BORROWED);
    }

    @Test
    @DisplayName("TC-EQE-17 เปลี่ยนสถานะของที่หาย → IllegalStateException (409)")
    void changeStatusWhileLostFails() {
        Equipment e = lost();
        assertThatThrownBy(() -> e.changeStatus(EquipmentStatus.AVAILABLE))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------- update (B-03)

    @Test
    @DisplayName("TC-EQE-18 update ของว่าง → เปลี่ยนประเภท barcode และย้ายจาก Counter เข้าห้องได้ สถานะเดิม")
    void update_ok() {
        Equipment e = newEquipment();
        EquipmentType adapter = new EquipmentType("Adapter USB-C", true);
        Resource room = Resource.bookable(new Zone("B"), "B-ROOM-01",
                com.cosync.booking.entity.enumtype.ServiceType.MEETING, 4, null, null);

        e.update(adapter, "EQ-F-900", room);

        assertThat(e.getEquipmentType()).isSameAs(adapter);
        assertThat(e.getBarcode()).isEqualTo("EQ-F-900");
        assertThat(e.getResource()).isSameAs(room);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQE-19 update ของที่กำลังซ่อม → ได้ (ย้ายกลับมา Counter: resource = null)")
    void update_maintenance() {
        Equipment e = newEquipment();
        e.changeStatus(EquipmentStatus.MAINTENANCE);

        e.update(e.getEquipmentType(), "EQ-L-999", null);

        assertThat(e.getBarcode()).isEqualTo("EQ-L-999");
        assertThat(e.getResource()).isNull();
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("TC-EQE-20 update ของที่ถูกยืม / หาย → IllegalStateException ค่าเดิมไม่เปลี่ยน")
    void update_borrowedOrLost() {
        Equipment b = borrowed();
        assertThatThrownBy(() -> b.update(b.getEquipmentType(), "NEW", null)).isInstanceOf(IllegalStateException.class);
        assertThat(b.getBarcode()).isEqualTo("EQ-L-001");

        Equipment l = lost();
        assertThatThrownBy(() -> l.update(l.getEquipmentType(), "NEW", null)).isInstanceOf(IllegalStateException.class);
        assertThat(l.getBarcode()).isEqualTo("EQ-L-001");
    }
}
