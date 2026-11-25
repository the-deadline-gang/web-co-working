package com.cosync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.equipment.EquipmentPricingRequest;
import com.cosync.booking.dto.equipment.EquipmentTypeResponse;
import com.cosync.booking.dto.rate.CreateEquipmentTypeRequest;
import com.cosync.booking.entity.EquipmentType;
import com.cosync.booking.repository.EquipmentTypeRepository;

@ExtendWith(MockitoExtension.class)
class EquipmentTypeServiceTest {

    @Mock EquipmentTypeRepository repo;

    EquipmentTypeService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentTypeService(repo);
    }

    private static EquipmentPricingRequest pricing(String fee, String deposit) {
        return new EquipmentPricingRequest(new BigDecimal(fee), new BigDecimal(deposit));
    }

    @Test
    @DisplayName("TC-EQT-01 list → แปลงทุกแถว")
    void list() {
        when(repo.findAll()).thenReturn(List.of(new EquipmentType("สาย HDMI", true), new EquipmentType("จอ TV", false)));

        assertThat(service.listEquipmentType()).extracting(EquipmentTypeResponse::name)
                .containsExactly("สาย HDMI", "จอ TV");
    }

    @Test
    @DisplayName("TC-EQT-02 create → บันทึกชื่อและ isLoanable ราคายังเป็น null")
    void create() {
        when(repo.save(any(EquipmentType.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentTypeResponse r = service.createEquipmentType(new CreateEquipmentTypeRequest("ไมค์ตั้งโต๊ะ", true));

        ArgumentCaptor<EquipmentType> saved = ArgumentCaptor.forClass(EquipmentType.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("ไมค์ตั้งโต๊ะ");
        assertThat(saved.getValue().isLoanable()).isTrue();
        assertThat(r.loanFee()).isNull();
        assertThat(r.deposit()).isNull();
    }

    @Test
    @DisplayName("TC-EQT-03 setPricing → ค่าใหม่อยู่ใน entity และ pricingUpdatedBy = STF-004")
    void setPricing_ok() {
        EquipmentType hdmi = new EquipmentType("สาย HDMI", true);
        when(repo.findById(1L)).thenReturn(Optional.of(hdmi));

        EquipmentTypeResponse r = service.setPricing(4L, 1L, pricing("25.00", "350.00"));

        assertThat(hdmi.getLoanFee()).isEqualByComparingTo("25.00");
        assertThat(hdmi.getDeposit()).isEqualByComparingTo("350.00");
        assertThat(hdmi.getPricingUpdatedBy()).isEqualTo("STF-004");
        assertThat(r.loanFee()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("TC-EQT-04 setPricing id ที่ไม่มี → 404 EQUIPMENT_TYPE_NOT_FOUND")
    void setPricing_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setPricing(4L, 99L, pricing("20", "300")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("EQUIPMENT_TYPE_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("TC-EQT-05 ตั้งราคาให้ของที่ยืมไม่ได้ (จอ TV) → 400 EQUIPMENT_TYPE_NOT_LOANABLE ราคายังเป็น null")
    void setPricing_notLoanable() {
        EquipmentType tv = new EquipmentType("จอ TV 55 นิ้ว", false);
        when(repo.findById(4L)).thenReturn(Optional.of(tv));

        assertThatThrownBy(() -> service.setPricing(4L, 4L, pricing("50", "5000")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("EQUIPMENT_TYPE_NOT_LOANABLE");
                });
        assertThat(tv.getLoanFee()).isNull();
        assertThat(tv.getDeposit()).isNull();
        assertThat(tv.getPricingUpdatedBy()).isNull();
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("TC-EQT-06 create ตัดเว้นวรรคหัวท้ายชื่อ")
    void create_trims() {
        when(repo.save(any(EquipmentType.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentTypeResponse r = service.createEquipmentType(new CreateEquipmentTypeRequest("  สาย HDMI  ", true));

        assertThat(r.name()).isEqualTo("สาย HDMI");
    }
}
