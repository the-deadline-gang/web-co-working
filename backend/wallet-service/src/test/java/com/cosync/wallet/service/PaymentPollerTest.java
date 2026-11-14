package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.WalletTestData.NOW;
import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static com.cosync.wallet.testsupport.WalletTestData.withStatus;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.payment.PaymentGateway;
import com.cosync.wallet.payment.PaymentResult;
import com.cosync.wallet.repository.TopupRepository;

@ExtendWith(MockitoExtension.class)
class PaymentPollerTest {

    private static final PaymentResult PAID = new PaymentResult("mock_ref_7", "successful", 500, NOW);

    @Mock TopupRepository topupRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock PaymentWebhookService paymentWebhookService;

    @InjectMocks PaymentPoller poller;

    private static Topup pending() {
        return topup(7L, wallet(1L, 4L, "0.00"), 500, NOW.plusSeconds(900));
    }

    @Test
    @DisplayName("TC-POL-01 gateway ตอบว่าจ่ายแล้ว → ส่งผลให้ applyPolled")
    void poll_paid_applies() {
        when(topupRepository.findById(7L)).thenReturn(Optional.of(pending()));
        when(paymentGateway.fetchStatus("mock_ref_7")).thenReturn(Optional.of(PAID));

        poller.pollFor(4L, 7L);

        verify(paymentWebhookService).applyPolled(PAID);
    }

    @Test
    @DisplayName("TC-POL-02 gateway ยังไม่มีผล (pending / ถามไม่ได้ / mock) → ไม่บันทึกอะไร")
    void poll_noResult() {
        when(topupRepository.findById(7L)).thenReturn(Optional.of(pending()));
        when(paymentGateway.fetchStatus("mock_ref_7")).thenReturn(Optional.empty());

        poller.pollFor(4L, 7L);

        verifyNoInteractions(paymentWebhookService);
    }

    @Test
    @DisplayName("TC-POL-03 topup ของคนอื่น → ไม่ถาม gateway (ให้ TopupService.get ตอบ 404)")
    void pollFor_otherUser() {
        when(topupRepository.findById(7L)).thenReturn(Optional.of(pending()));

        poller.pollFor(5L, 7L);

        verifyNoInteractions(paymentGateway, paymentWebhookService);
    }

    @Test
    @DisplayName("TC-POL-04 ไม่มี topup → ไม่ถาม gateway")
    void pollFor_notFound() {
        when(topupRepository.findById(404L)).thenReturn(Optional.empty());

        poller.pollFor(4L, 404L);

        verifyNoInteractions(paymentGateway, paymentWebhookService);
    }

    @ParameterizedTest
    @EnumSource(value = TopupStatus.class, names = {"PAID", "FAILED"})
    @DisplayName("TC-POL-05 PAID / FAILED แล้ว → ไม่ถาม gateway ซ้ำ")
    void poll_finished_skips(TopupStatus status) {
        poller.poll(withStatus(pending(), status));

        verifyNoInteractions(paymentGateway, paymentWebhookService);
    }

    @Test
    @DisplayName("TC-POL-06 EXPIRED ยังถาม (คนจ่ายช้ายังได้ Token ตาม Q1 = A)")
    void poll_expired_stillAsks() {
        when(paymentGateway.fetchStatus("mock_ref_7")).thenReturn(Optional.of(PAID));

        poller.poll(withStatus(pending(), TopupStatus.EXPIRED));

        verify(paymentWebhookService).applyPolled(PAID);
    }

    @Test
    @DisplayName("TC-POL-07 poll ชนกับอีกทางจน PK ซ้ำ → ไม่โยน error ให้ผู้ใช้")
    void poll_duplicate_swallowed() {
        when(paymentGateway.fetchStatus("mock_ref_7")).thenReturn(Optional.of(PAID));
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(paymentWebhookService).applyPolled(any());

        assertDoesNotThrow(() -> poller.poll(pending()));
        verify(paymentWebhookService).applyPolled(PAID);
        verify(topupRepository, never()).findById(any());
    }
}
