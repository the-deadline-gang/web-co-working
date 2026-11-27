package com.cosync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import com.cosync.booking.dto.equipment.EquipmentRequest;
import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.entity.Equipment;
import com.cosync.booking.entity.EquipmentType;
import com.cosync.booking.entity.Resource;
import com.cosync.booking.entity.Zone;
import com.cosync.booking.entity.enumtype.EquipmentStatus;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.repository.EquipmentRepository;
import com.cosync.booking.repository.EquipmentTypeRepository;
import com.cosync.booking.repository.LoanRepository;
import com.cosync.booking.repository.ResourceRepository;

/**
 * กฎของ /admin/equipment
 * - resourceId = null คือของที่ Counter (ห้ามเรียก findById(null))
 * - ของที่ถูกยืม / หาย แก้ไม่ได้ (409) · status ที่ส่งมาได้เฉพาะ AVAILABLE / MAINTENANCE (400) · ไม่ส่ง = ไม่เปลี่ยน
 * - ลบได้เฉพาะชิ้นที่ไม่เคยมี loan
 */
@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock EquipmentRepository equipmentRepo;
    @Mock ResourceRepository resourceRepo;
    @Mock EquipmentTypeRepository typeRepo;
    @Mock LoanRepository loanRepo;

    EquipmentService service;

    EquipmentType hdmi;      // id 200 (เกิน 127 ตั้งใจ จับการเทียบ Long ด้วย ==)
    EquipmentType tv;        // id 300
    Resource room;
    Equipment counterCable;  // EQ-L-001 ที่ Counter
    Equipment roomTv;        // EQ-F-001 ประจำห้อง

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepo, resourceRepo, typeRepo, loanRepo);
        hdmi = withId(new EquipmentType("สาย HDMI", true), 200L);
        tv = withId(new EquipmentType("จอ TV 55 นิ้ว", false), 300L);
        room = Resource.bookable(new Zone("B"), "B-ROOM-01", ServiceType.MEETING, 4, null, null);
        counterCable = new Equipment(hdmi, "EQ-L-001", null);
        roomTv = new Equipment(tv, "EQ-F-001", room);
    }

    private static EquipmentType withId(EquipmentType t, Long id) {
        ReflectionTestUtils.setField(t, "equipmentTypeId", id);
        return t;
    }

    private static EquipmentRequest req(Long typeId, String barcode, Long resourceId, EquipmentStatus status) {
        return new EquipmentRequest(typeId, barcode, resourceId, status);
    }

    private static void assertApi(HttpStatus status, String code, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }

    // ---------------------------------------------------------------- list

    @Test
    @DisplayName("TC-EQS-01 list ไม่กรอง / กรองสถานะ / กรองประเภท (typeId 200 จาก URL)")
    void list_filters() {
        roomTv.changeStatus(EquipmentStatus.MAINTENANCE);
        when(equipmentRepo.findAll()).thenReturn(List.of(counterCable, roomTv));

        assertThat(service.listEquipment(null, null)).hasSize(2);
        assertThat(service.listEquipment(EquipmentStatus.MAINTENANCE, null))
                .extracting(EquipmentResponse::barcode).containsExactly("EQ-F-001");
        assertThat(service.listEquipment(null, Long.valueOf("200")))
                .extracting(EquipmentResponse::barcode).containsExactly("EQ-L-001");
        assertThat(service.listEquipment(EquipmentStatus.MAINTENANCE, 200L)).isEmpty();
    }

    @Test
    @DisplayName("TC-EQS-02 response ของที่ Counter → resourceCode เป็น null (ไม่ NPE) · ของประจำห้อง → code ของห้อง")
    void list_resourceCode() {
        when(equipmentRepo.findAll()).thenReturn(List.of(counterCable, roomTv));

        List<EquipmentResponse> r = service.listEquipment(null, null);

        assertThat(r.get(0).resourceCode()).isNull();
        assertThat(r.get(1).resourceCode()).isEqualTo("B-ROOM-01");
        assertThat(r.get(0).typeName()).isEqualTo("สาย HDMI");
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("TC-EQS-03 สร้างของที่ Counter (resourceId = null) → ไม่ไปหา resource · trim barcode · AVAILABLE")
    void create_counterItem() {
        when(typeRepo.findById(200L)).thenReturn(Optional.of(hdmi));

        EquipmentResponse r = service.createEquipment(req(200L, "  EQ-L-004 ", null, null));

        verifyNoInteractions(resourceRepo);
        ArgumentCaptor<Equipment> saved = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepo).save(saved.capture());
        assertThat(saved.getValue().getBarcode()).isEqualTo("EQ-L-004");
        assertThat(saved.getValue().getResource()).isNull();
        assertThat(r.status()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQS-04 สร้างของประจำห้อง → ผูก resource")
    void create_roomItem() {
        when(typeRepo.findById(300L)).thenReturn(Optional.of(tv));
        when(resourceRepo.findById(6L)).thenReturn(Optional.of(room));

        EquipmentResponse r = service.createEquipment(req(300L, "EQ-F-009", 6L, null));

        assertThat(r.resourceCode()).isEqualTo("B-ROOM-01");
    }

    @Test
    @DisplayName("TC-EQS-05 ส่ง status = BORROWED ตอนสร้าง → ไม่สนใจ ได้ AVAILABLE")
    void create_ignoresStatus() {
        when(typeRepo.findById(200L)).thenReturn(Optional.of(hdmi));

        EquipmentResponse r = service.createEquipment(req(200L, "EQ-L-005", null, EquipmentStatus.BORROWED));

        assertThat(r.status()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQS-06 ประเภท / resource ไม่มีจริง → 404 ไม่ save")
    void create_notFound() {
        when(typeRepo.findById(999L)).thenReturn(Optional.empty());
        assertApi(HttpStatus.NOT_FOUND, "EQUIPMENT_TYPE_NOT_FOUND",
                () -> service.createEquipment(req(999L, "EQ-X", null, null)));

        when(resourceRepo.findById(888L)).thenReturn(Optional.empty());
        assertApi(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                () -> service.createEquipment(req(200L, "EQ-X", 888L, null)));

        verify(equipmentRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("TC-EQS-07 แก้แค่ barcode ไม่ส่ง status → สำเร็จ สถานะเดิม (MAINTENANCE ยังเป็น MAINTENANCE)")
    void update_noStatus() {
        counterCable.changeStatus(EquipmentStatus.MAINTENANCE);
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));
        when(typeRepo.findById(200L)).thenReturn(Optional.of(hdmi));

        EquipmentResponse r = service.updateEquipment(1L, req(200L, " EQ-L-001A ", null, null));

        assertThat(r.barcode()).isEqualTo("EQ-L-001A");
        assertThat(r.status()).isEqualTo(EquipmentStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("TC-EQS-08 ซ่อมเสร็จ: MAINTENANCE → AVAILABLE")
    void update_backToAvailable() {
        counterCable.changeStatus(EquipmentStatus.MAINTENANCE);
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));
        when(typeRepo.findById(200L)).thenReturn(Optional.of(hdmi));

        EquipmentResponse r = service.updateEquipment(1L, req(200L, "EQ-L-001", null, EquipmentStatus.AVAILABLE));

        assertThat(r.status()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQS-09 ส่ง status = LOST / BORROWED → 400 สถานะไม่เปลี่ยน")
    void update_badStatus() {
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));
        when(typeRepo.findById(200L)).thenReturn(Optional.of(hdmi));

        assertApi(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> service.updateEquipment(1L, req(200L, "EQ-L-001", null, EquipmentStatus.LOST)));
        assertApi(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> service.updateEquipment(1L, req(200L, "EQ-L-001", null, EquipmentStatus.BORROWED)));
        assertThat(counterCable.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("TC-EQS-10 ของที่ถูกยืม → 409 EQUIPMENT_IN_USE ก่อนไปหา resource / ประเภท")
    void update_borrowed() {
        counterCable.markBorrowed();
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));

        assertApi(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE",
                () -> service.updateEquipment(1L, req(999L, "NEW", 888L, null)));
        verifyNoInteractions(typeRepo, resourceRepo);
        assertThat(counterCable.getBarcode()).isEqualTo("EQ-L-001");
    }

    @Test
    @DisplayName("TC-EQS-11 ของที่หาย (LOST) → 409 ไม่ใช่ 500")
    void update_lost() {
        counterCable.markBorrowed();
        counterCable.markLost();
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));

        assertApi(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE",
                () -> service.updateEquipment(1L, req(200L, "NEW", null, null)));
    }

    @Test
    @DisplayName("TC-EQS-12 อุปกรณ์ไม่มีจริง → 404 EQUIPMENT_NOT_FOUND")
    void update_notFound() {
        when(equipmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertApi(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND",
                () -> service.updateEquipment(99L, req(200L, "X", null, null)));
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("TC-EQS-13 ลบของที่ยังไม่เคยถูกยืม → ลบได้")
    void delete_ok() {
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));
        when(loanRepo.existsByEquipment(counterCable)).thenReturn(false);

        service.deleteEquipment(1L);

        verify(equipmentRepo).delete(counterCable);
    }

    @Test
    @DisplayName("TC-EQS-14 ลบของที่ถูกยืมอยู่ → 409 EQUIPMENT_IN_USE (ไม่ใช่ HAS_HISTORY แม้จะมี loan อยู่)")
    void delete_borrowed() {
        counterCable.markBorrowed();
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));

        assertApi(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE", () -> service.deleteEquipment(1L));
        verify(equipmentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("TC-EQS-15 ลบของที่เคยถูกยืม (คืนแล้ว) → 409 EQUIPMENT_HAS_HISTORY")
    void delete_hasHistory() {
        when(equipmentRepo.findById(1L)).thenReturn(Optional.of(counterCable));
        when(loanRepo.existsByEquipment(counterCable)).thenReturn(true);

        assertApi(HttpStatus.CONFLICT, "EQUIPMENT_HAS_HISTORY", () -> service.deleteEquipment(1L));
        verify(equipmentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("TC-EQS-16 ลบ id ที่ไม่มี → 404")
    void delete_notFound() {
        when(equipmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertApi(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", () -> service.deleteEquipment(99L));
    }
}
