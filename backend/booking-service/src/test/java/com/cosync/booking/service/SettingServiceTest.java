package com.cosync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.setting.BookingSettingsRequest;
import com.cosync.booking.dto.setting.BookingSettingsResponse;
import com.cosync.booking.entity.BookingSetting;
import com.cosync.booking.repository.BookingSettingRepository;

/** กฎธุรกิจของการแก้ค่าตั้ง: ตรวจเวลา, เกณฑ์คืนเงิน, updatedBy และ flush */
@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock BookingSettingRepository repo;

    SettingService service;
    BookingSetting setting;

    @BeforeEach
    void setUp() throws Exception {
        service = new SettingService(repo);
        // constructor ของ entity เป็น protected (ไว้ให้ Hibernate) เลยต้องสร้างผ่าน reflection
        Constructor<BookingSetting> c = BookingSetting.class.getDeclaredConstructor();
        c.setAccessible(true);
        setting = c.newInstance();
        setting.update(request(LocalTime.of(8, 0), LocalTime.of(22, 0), 24, 2), "SYSTEM");
    }

    private static BookingSettingsRequest request(LocalTime open, LocalTime close, int fullHours, int halfHours) {
        return new BookingSettingsRequest(open, close, 30, 10, 15, 15, 30, fullHours, halfHours, 3, 3);
    }

    private void assertBadRequest(BookingSettingsRequest req) {
        assertThatThrownBy(() -> service.updateSettings(4L, req))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("VALIDATION_FAILED");
                });
        verify(repo, never()).flush();
    }

    @Test
    @DisplayName("TC-BST-01 ดึงค่าตั้ง → ได้ค่าจากแถว id = 1")
    void getSettings() {
        when(repo.findById(BookingSetting.SINGLETON_ID)).thenReturn(Optional.of(setting));

        BookingSettingsResponse r = service.getSettings();

        assertThat(r.openTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(r.cancelFullRefundHours()).isEqualTo(24);
        assertThat(r.cancelHalfRefundHours()).isEqualTo(2);
        assertThat(r.updatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("TC-BST-02 แก้ค่า → ค่าใหม่ถูกบันทึก updatedBy = STF-004 แล้ว flush ก่อนสร้าง response")
    void update_ok() {
        when(repo.findById(BookingSetting.SINGLETON_ID)).thenReturn(Optional.of(setting));

        BookingSettingsResponse r = service.updateSettings(4L,
                request(LocalTime.of(9, 0), LocalTime.of(21, 0), 48, 6));

        assertThat(r.openTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(r.closeTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(r.cancelFullRefundHours()).isEqualTo(48);
        assertThat(r.cancelHalfRefundHours()).isEqualTo(6);
        assertThat(r.updatedBy()).isEqualTo("STF-004");
        InOrder order = inOrder(repo);
        order.verify(repo).findById(BookingSetting.SINGLETON_ID);
        order.verify(repo).flush();
    }

    @Test
    @DisplayName("TC-BST-03 เวลาเปิดหลังเวลาปิด → 400")
    void openAfterClose() {
        assertBadRequest(request(LocalTime.of(22, 0), LocalTime.of(8, 0), 24, 2));
    }

    @Test
    @DisplayName("TC-BST-04 เวลาเปิดเท่ากับเวลาปิด → 400 (DB CHECK เป็น < ถ้าหลุดไปจะได้ 500)")
    void openEqualsClose() {
        assertBadRequest(request(LocalTime.of(8, 0), LocalTime.of(8, 0), 24, 2));
    }

    @Test
    @DisplayName("TC-BST-05 คืน 50% มากกว่าเกณฑ์คืน 100% → 400")
    void halfGreaterThanFull() {
        assertBadRequest(request(LocalTime.of(8, 0), LocalTime.of(22, 0), 2, 24));
    }

    @Test
    @DisplayName("TC-BST-06 คืน 50% เท่ากับเกณฑ์คืน 100% → 400 (ขอบที่เคยหลุดตอนใช้ >)")
    void halfEqualsFull() {
        assertBadRequest(request(LocalTime.of(8, 0), LocalTime.of(22, 0), 24, 24));
    }

    @Test
    @DisplayName("TC-BST-07 คืน 50% = 0 ชม. (คืนครึ่งได้จนถึงเวลาเริ่ม) → ผ่าน")
    void halfZeroAllowed() {
        when(repo.findById(BookingSetting.SINGLETON_ID)).thenReturn(Optional.of(setting));

        BookingSettingsResponse r = service.updateSettings(4L,
                request(LocalTime.of(8, 0), LocalTime.of(22, 0), 24, 0));

        assertThat(r.cancelHalfRefundHours()).isZero();
    }

    @Test
    @DisplayName("TC-BST-08 ไม่มีแถวค่าตั้ง (ลืม seed V2) → exception ที่กลายเป็น 500 ไม่ใช่ 4xx")
    void missingRow() {
        when(repo.findById(BookingSetting.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSettings()).isNotInstanceOf(ApiException.class);
    }
}
