package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.WalletTestData.NOW;
import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.repository.TopupRepository;

@ExtendWith(MockitoExtension.class)
class OmiseChargePollJobTest {

    @Mock TopupRepository topupRepository;
    @Mock PaymentPoller paymentPoller;

    @InjectMocks OmiseChargePollJob job;

    private static Topup pending(long id) {
        return topup(id, wallet(1L, 4L, "0.00"), 500, NOW.plusSeconds(900));
    }

    @Test
    @DisplayName("TC-OPJ-01 ค้นเฉพาะ PENDING กับ EXPIRED ที่สร้างภายใน 2 ชม. ล่าสุด")
    @SuppressWarnings("unchecked")
    void queriesOpenTopupsWithinWindow() {
        when(topupRepository.findByStatusInAndCreatedAtAfter(anyCollection(), any())).thenReturn(List.of());
        Instant before = Instant.now();

        job.pollOpenCharges();

        ArgumentCaptor<Collection<TopupStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(topupRepository).findByStatusInAndCreatedAtAfter(statuses.capture(), since.capture());

        assertEquals(Set.of(TopupStatus.PENDING, TopupStatus.EXPIRED), Set.copyOf(statuses.getValue()));
        Duration lookBack = Duration.between(since.getValue(), before);
        assertFalse(lookBack.compareTo(Duration.ofHours(2)) > 0, "ย้อนหลังไม่เกิน 2 ชม. แต่ได้ " + lookBack);
        assertTrue(lookBack.compareTo(Duration.ofHours(2).minusSeconds(5)) >= 0, "ย้อนหลังประมาณ 2 ชม. แต่ได้ " + lookBack);
    }

    @Test
    @DisplayName("TC-OPJ-02 มีรายการค้าง → ถาม gateway ทุกรายการ")
    void pollsEveryOpenTopup() {
        Topup a = pending(7L);
        Topup b = pending(8L);
        when(topupRepository.findByStatusInAndCreatedAtAfter(anyCollection(), any())).thenReturn(List.of(a, b));

        job.pollOpenCharges();

        verify(paymentPoller).poll(a);
        verify(paymentPoller).poll(b);
    }

    @Test
    @DisplayName("TC-OPJ-03 รายการหนึ่งพัง → รายการถัดไปยังถูกถาม และ job ไม่โยน error")
    void oneFailureDoesNotStopOthers() {
        Topup broken = pending(7L);
        Topup ok = pending(8L);
        when(topupRepository.findByStatusInAndCreatedAtAfter(anyCollection(), any())).thenReturn(List.of(broken, ok));
        doThrow(new IllegalStateException("DB สะดุด")).when(paymentPoller).poll(broken);

        job.pollOpenCharges();

        verify(paymentPoller).poll(ok);
    }

    @Test
    @DisplayName("TC-OPJ-04 ไม่มีรายการค้าง → ไม่เรียก poller")
    void nothingOpen() {
        when(topupRepository.findByStatusInAndCreatedAtAfter(anyCollection(), any())).thenReturn(List.of());

        job.pollOpenCharges();

        verifyNoInteractions(paymentPoller);
    }
}
