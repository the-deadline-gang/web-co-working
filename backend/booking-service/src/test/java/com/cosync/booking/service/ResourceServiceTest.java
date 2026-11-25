package com.cosync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.resource.ResourceRequest;
import com.cosync.booking.dto.resource.ResourceResponse;
import com.cosync.booking.entity.Resource;
import com.cosync.booking.entity.Zone;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.repository.ResourceRepository;
import com.cosync.booking.repository.ZoneRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock ResourceRepository resourceRepo;
    @Mock ZoneRepository zoneRepo;

    ResourceService service;

    /** id เกิน 127 ตั้งใจ: Long ที่ค่า > 127 เทียบด้วย == จะได้ false แม้ค่าเท่ากัน ต้องใช้ equals() */
    Zone zoneA;
    Zone zoneB;
    Resource dayPass;
    Resource desk;
    Resource room;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resourceRepo, zoneRepo);
        zoneA = zone(200L, "A");
        zoneB = zone(300L, "B");
        dayPass = Resource.dayPass(zoneA, "A-DAYPASS", 40, 40, null, null);
        desk = Resource.bookable(zoneA, "A-DESK-01", ServiceType.DESK, 1, null, null);
        room = Resource.bookable(zoneB, "B-ROOM-01", ServiceType.MEETING, 4, new BigDecimal("12.00"), null);
    }

    private static Zone zone(Long id, String name) {
        Zone z = new Zone(name);
        ReflectionTestUtils.setField(z, "id", id);      // id ปกติ DB เป็นคนสร้าง
        return z;
    }

    private static ResourceRequest request(String code, Long zoneId, ServiceType type, Integer capacity,
                                           Integer quota, Boolean active) {
        return new ResourceRequest(code, zoneId, type, capacity, quota, null, null, active);
    }

    private static void assertApi(HttpStatus status, String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }

    // ---------------------------------------------------------------- list

    @Test
    @DisplayName("TC-RSC-01 ไม่มีตัวกรอง → ได้ทุกตัว พร้อม zoneName และ fixedEquipment ว่าง")
    void list_all() {
        when(resourceRepo.findAll()).thenReturn(List.of(dayPass, desk, room));

        List<ResourceResponse> r = service.listResource(null, null);

        assertThat(r).extracting(ResourceResponse::code).containsExactly("A-DAYPASS", "A-DESK-01", "B-ROOM-01");
        assertThat(r.get(2).zoneName()).isEqualTo("B");
        assertThat(r.get(2).fixedEquipment()).isEmpty();
    }

    @Test
    @DisplayName("TC-RSC-02 กรองประเภทอย่างเดียว")
    void list_byType() {
        when(resourceRepo.findAll()).thenReturn(List.of(dayPass, desk, room));

        assertThat(service.listResource(ServiceType.MEETING, null))
                .extracting(ResourceResponse::code).containsExactly("B-ROOM-01");
    }

    @Test
    @DisplayName("TC-RSC-03 กรองโซนอย่างเดียว (id 200 > 127 ถ้าใช้ == แทน equals() จะได้ list ว่าง)")
    void list_byZone() {
        when(resourceRepo.findAll()).thenReturn(List.of(dayPass, desk, room));

        Long zoneIdFromUrl = Long.valueOf("200");   // แปลงจากข้อความแบบที่ Spring ทำกับ query param → คนละ object กับ id ใน Zone

        assertThat(service.listResource(null, zoneIdFromUrl))
                .extracting(ResourceResponse::code).containsExactly("A-DAYPASS", "A-DESK-01");
    }

    @Test
    @DisplayName("TC-RSC-04 กรองทั้งคู่ → ต้องตรงทั้ง 2 เงื่อนไข · ไม่มีที่ตรงได้ list ว่าง")
    void list_bothFilters() {
        when(resourceRepo.findAll()).thenReturn(List.of(dayPass, desk, room));

        assertThat(service.listResource(ServiceType.DESK, 200L))
                .extracting(ResourceResponse::code).containsExactly("A-DESK-01");
        assertThat(service.listResource(ServiceType.GAME, 200L)).isEmpty();
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("TC-RSC-05 สร้างห้องประชุม → bookable, quota null, trim code, save")
    void create_meeting() {
        when(zoneRepo.findById(300L)).thenReturn(Optional.of(zoneB));

        ResourceResponse r = service.creatResource(request("  B-ROOM-03 ", 300L, ServiceType.MEETING, 6, null, null));

        ArgumentCaptor<Resource> saved = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepo).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("B-ROOM-03");
        assertThat(saved.getValue().getDailyQuota()).isNull();
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(r.zoneName()).isEqualTo("B");
    }

    @Test
    @DisplayName("TC-RSC-06 สร้าง Day Pass พร้อม quota → dayPass")
    void create_dayPass() {
        when(zoneRepo.findById(200L)).thenReturn(Optional.of(zoneA));

        ResourceResponse r = service.creatResource(request("A-DP-2", 200L, ServiceType.DAY_PASS, 30, 30, null));

        assertThat(r.serviceType()).isEqualTo(ServiceType.DAY_PASS);
        assertThat(r.dailyQuota()).isEqualTo(30);
    }

    @Test
    @DisplayName("TC-RSC-07 Day Pass ไม่มี quota → 400 ก่อนไปหาโซน")
    void create_dayPassWithoutQuota() {
        assertApi(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> service.creatResource(request("A-DP-2", 200L, ServiceType.DAY_PASS, 30, null, null)));
        verifyNoInteractions(zoneRepo, resourceRepo);
    }

    @Test
    @DisplayName("TC-RSC-08 โต๊ะแต่ส่ง quota มา → 400")
    void create_deskWithQuota() {
        assertApi(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> service.creatResource(request("A-DESK-09", 200L, ServiceType.DESK, 1, 5, null)));
        verifyNoInteractions(zoneRepo, resourceRepo);
    }

    @Test
    @DisplayName("TC-RSC-09 โซนไม่มีจริง → 404 ZONE_NOT_FOUND ไม่ save")
    void create_zoneNotFound() {
        when(zoneRepo.findById(999L)).thenReturn(Optional.empty());

        assertApi(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND",
                () -> service.creatResource(request("X-1", 999L, ServiceType.DESK, 1, null, null)));
        verify(resourceRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("TC-RSC-10 แก้ห้อง ย้ายโซน trim code")
    void update_ok() {
        when(resourceRepo.findById(6L)).thenReturn(Optional.of(room));
        when(zoneRepo.findById(200L)).thenReturn(Optional.of(zoneA));

        ResourceResponse r = service.update(6L, request(" A-ROOM-01 ", 200L, ServiceType.MEETING, 8, null, true));

        assertThat(room.getCode()).isEqualTo("A-ROOM-01");
        assertThat(room.getZone()).isSameAs(zoneA);
        assertThat(room.getCapacity()).isEqualTo(8);
        assertThat(r.zoneName()).isEqualTo("A");
    }

    @Test
    @DisplayName("TC-RSC-11 isActive = false → ปิดห้อง")
    void update_deactivate() {
        when(resourceRepo.findById(6L)).thenReturn(Optional.of(room));
        when(zoneRepo.findById(300L)).thenReturn(Optional.of(zoneB));

        service.update(6L, request("B-ROOM-01", 300L, ServiceType.MEETING, 4, null, false));

        assertThat(room.isActive()).isFalse();
    }

    @Test
    @DisplayName("TC-RSC-12 ห้องที่ปิดอยู่ ส่ง PUT แก้ capacity โดยไม่ส่ง isActive → ยังปิดอยู่ (null = ไม่เปลี่ยน)")
    void update_isActiveNullKeepsValue() {
        room.update(zoneB, "B-ROOM-01", ServiceType.MEETING, 4, null, null, null, false);
        when(resourceRepo.findById(6L)).thenReturn(Optional.of(room));
        when(zoneRepo.findById(300L)).thenReturn(Optional.of(zoneB));

        service.update(6L, request("B-ROOM-01", 300L, ServiceType.MEETING, 8, null, null));

        assertThat(room.getCapacity()).isEqualTo(8);
        assertThat(room.isActive()).isFalse();
    }

    @Test
    @DisplayName("TC-RSC-13 resource ไม่มีจริง → 404 RESOURCE_NOT_FOUND")
    void update_notFound() {
        when(resourceRepo.findById(99L)).thenReturn(Optional.empty());

        assertApi(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                () -> service.update(99L, request("B-ROOM-01", 300L, ServiceType.MEETING, 4, null, null)));
    }

    @Test
    @DisplayName("TC-RSC-14 เปลี่ยนห้องเป็น DAY_PASS แต่ไม่ส่ง quota → 400 (ไม่ใช่ 500 จาก entity) ค่าเดิมไม่เปลี่ยน")
    void update_toDayPassWithoutQuota() {
        assertApi(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> service.update(6L, request("B-DP", 300L, ServiceType.DAY_PASS, 20, null, null)));
        assertThat(room.getServiceType()).isEqualTo(ServiceType.MEETING);
    }
}
