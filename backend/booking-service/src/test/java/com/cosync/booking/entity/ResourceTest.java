package com.cosync.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cosync.booking.entity.enumtype.ServiceType;

/**
 * กฎ quota ของ Resource: DAY_PASS ต้องมี dailyQuota ≥ 1 / ประเภทอื่นต้องเป็น null
 * ต้องใช้กฎเดียวกันทั้งตอนสร้าง (dayPass, bookable) และตอนแก้ (update)
 */
class ResourceTest {

    private final Zone zoneA = new Zone("A");
    private final Zone zoneB = new Zone("B");

    private Resource meetingRoom() {
        return Resource.bookable(zoneB, "B-ROOM-01", ServiceType.MEETING, 4, new BigDecimal("12.00"), null);
    }

    // ---------------------------------------------------------------- สร้าง

    @Test
    @DisplayName("TC-RSE-01 dayPass → DAY_PASS มี quota เปิดใช้งาน")
    void dayPass_ok() {
        Resource r = Resource.dayPass(zoneA, "A-DAYPASS", 40, 40, new BigDecimal("120.00"), null);

        assertThat(r.getServiceType()).isEqualTo(ServiceType.DAY_PASS);
        assertThat(r.getDailyQuota()).isEqualTo(40);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    @DisplayName("TC-RSE-02 dayPass quota = 1 (ขอบล่าง) → ผ่าน")
    void dayPass_quotaOne() {
        assertThat(Resource.dayPass(zoneA, "A-DP-SMALL", 1, 1, null, null).getDailyQuota()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-RSE-03 dayPass quota = 0 → IllegalArgumentException")
    void dayPass_quotaZero() {
        assertThatThrownBy(() -> Resource.dayPass(zoneA, "A-DP", 40, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC-RSE-04 bookable → ไม่มี quota เปิดใช้งาน")
    void bookable_ok() {
        Resource r = meetingRoom();

        assertThat(r.getServiceType()).isEqualTo(ServiceType.MEETING);
        assertThat(r.getDailyQuota()).isNull();
        assertThat(r.getCapacity()).isEqualTo(4);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    @DisplayName("TC-RSE-05 bookable ด้วย DAY_PASS → IllegalArgumentException (ต้องใช้ dayPass)")
    void bookable_dayPassRejected() {
        assertThatThrownBy(() -> Resource.bookable(zoneA, "A-X", ServiceType.DAY_PASS, 40, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- แก้

    @Test
    @DisplayName("TC-RSE-06 update ห้องประชุม → ค่าใหม่ครบทุกช่อง ย้ายโซนได้")
    void update_ok() {
        Resource r = meetingRoom();

        r.update(zoneA, "A-ROOM-09", ServiceType.STUDIO, 6, null, new BigDecimal("20.50"), "https://img/x.png", true);

        assertThat(r.getZone()).isSameAs(zoneA);
        assertThat(r.getCode()).isEqualTo("A-ROOM-09");
        assertThat(r.getServiceType()).isEqualTo(ServiceType.STUDIO);
        assertThat(r.getCapacity()).isEqualTo(6);
        assertThat(r.getDailyQuota()).isNull();
        assertThat(r.getSizeSqm()).isEqualByComparingTo("20.50");
        assertThat(r.getImageUrl()).isEqualTo("https://img/x.png");
    }

    @Test
    @DisplayName("TC-RSE-07 update ปิดใช้งานแล้วเปิดกลับได้")
    void update_deactivateAndReactivate() {
        Resource r = meetingRoom();

        r.update(zoneB, "B-ROOM-01", ServiceType.MEETING, 4, null, null, null, false);
        assertThat(r.isActive()).isFalse();

        r.update(zoneB, "B-ROOM-01", ServiceType.MEETING, 4, null, null, null, true);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    @DisplayName("TC-RSE-08 update เปลี่ยนห้องเป็น DAY_PASS พร้อม quota → ผ่าน")
    void update_toDayPassWithQuota() {
        Resource r = meetingRoom();

        r.update(zoneB, "B-DAYPASS", ServiceType.DAY_PASS, 20, 20, null, null, true);

        assertThat(r.getServiceType()).isEqualTo(ServiceType.DAY_PASS);
        assertThat(r.getDailyQuota()).isEqualTo(20);
    }

    @Test
    @DisplayName("TC-RSE-09 update เป็น DAY_PASS แต่ไม่มี quota (null) → IllegalArgumentException ไม่ใช่ NPE และค่าเดิมไม่เปลี่ยน")
    void update_toDayPassWithoutQuota() {
        Resource r = meetingRoom();

        assertThatThrownBy(() -> r.update(zoneB, "B-DAYPASS", ServiceType.DAY_PASS, 20, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(r.getServiceType()).isEqualTo(ServiceType.MEETING);
        assertThat(r.getCode()).isEqualTo("B-ROOM-01");
    }

    @Test
    @DisplayName("TC-RSE-10 update ห้องประชุมแต่ใส่ quota มา → IllegalArgumentException และค่าเดิมไม่เปลี่ยน")
    void update_meetingWithQuota() {
        Resource r = meetingRoom();

        assertThatThrownBy(() -> r.update(zoneB, "B-ROOM-01", ServiceType.MEETING, 8, 40, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(r.getCapacity()).isEqualTo(4);
        assertThat(r.getDailyQuota()).isNull();
    }

    @Test
    @DisplayName("TC-RSE-11 update DAY_PASS เป็นโต๊ะ ต้องล้าง quota เป็น null ด้วย")
    void update_dayPassToDesk() {
        Resource r = Resource.dayPass(zoneA, "A-DAYPASS", 40, 40, null, null);

        assertThatThrownBy(() -> r.update(zoneA, "A-DESK-99", ServiceType.DESK, 1, 40, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);

        r.update(zoneA, "A-DESK-99", ServiceType.DESK, 1, null, null, null, true);
        assertThat(r.getDailyQuota()).isNull();
    }
}
