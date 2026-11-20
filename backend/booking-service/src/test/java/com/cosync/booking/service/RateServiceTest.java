package com.cosync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.rate.RateResponse;
import com.cosync.booking.dto.rate.RateUpdateRequest;
import com.cosync.booking.entity.ServiceRate;
import com.cosync.booking.entity.enumtype.RateUnit;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.repository.ServiceRateRepository;

@ExtendWith(MockitoExtension.class)
class RateServiceTest {

    @Mock ServiceRateRepository repo;

    RateService service;
    ServiceRate meeting;

    @BeforeEach
    void setUp() {
        service = new RateService(repo);
        meeting = new ServiceRate(ServiceType.MEETING, "ห้องประชุม", new BigDecimal("120.00"),
                RateUnit.HOUR, new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("TC-RTE-01 list → แปลงทุกแถวเป็น RateResponse")
    void list() {
        ServiceRate desk = new ServiceRate(ServiceType.DESK, "โต๊ะทำงาน", new BigDecimal("40.00"),
                RateUnit.HOUR, new BigDecimal("0.00"));
        when(repo.findAll()).thenReturn(List.of(desk, meeting));

        List<RateResponse> r = service.list();

        assertThat(r).extracting(RateResponse::serviceType)
                .containsExactly(ServiceType.DESK, ServiceType.MEETING);
    }

    @Test
    @DisplayName("TC-RTE-02 แก้ราคา → ค่าใหม่ updatedBy = STF-004 และ flush หลังหาเจอ")
    void update_ok() {
        when(repo.findById(ServiceType.MEETING)).thenReturn(Optional.of(meeting));

        RateResponse r = service.update(4L, ServiceType.MEETING,
                new RateUpdateRequest(new BigDecimal("150.00"), new BigDecimal("12.50")));

        assertThat(r.rate()).isEqualByComparingTo("150.00");
        assertThat(r.discountPct()).isEqualByComparingTo("12.50");
        assertThat(r.unit()).isEqualTo(RateUnit.HOUR);           // ไม่ได้แก้ unit
        assertThat(meeting.getUpdateBy()).isEqualTo("STF-004");
        InOrder order = inOrder(repo);
        order.verify(repo).findById(ServiceType.MEETING);
        order.verify(repo).flush();
    }

    @Test
    @DisplayName("TC-RTE-03 ไม่มีแถวของ serviceType นั้น → 404 RATE_NOT_FOUND ไม่ flush")
    void update_notFound() {
        when(repo.findById(ServiceType.GAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(4L, ServiceType.GAME,
                        new RateUpdateRequest(new BigDecimal("150"), BigDecimal.ZERO)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("RATE_NOT_FOUND");
                });
        verify(repo, never()).flush();
    }
}
