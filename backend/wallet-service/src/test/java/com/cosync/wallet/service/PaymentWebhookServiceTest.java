package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.WalletTestData.NOW;
import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static com.cosync.wallet.testsupport.WalletTestData.withStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cosync.wallet.dto.webhook.PaymentWebhook;
import com.cosync.wallet.payment.PaymentResult;
import com.cosync.wallet.entity.PaymentEvent;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletTransaction;
import com.cosync.wallet.repository.PaymentEventRepository;
import com.cosync.wallet.repository.TopupRepository;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletTransactionRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    private static final String RAW = "{\"eventId\":\"evt_1\"}";

    @Mock PaymentEventRepository paymentEventRepository;
    @Mock TopupRepository topupRepository;
    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;

    @Spy JsonMapper jsonMapper = JsonMapper.builder().build();   // ของจริง ใช้แปลงผล poll เป็น payload

    @InjectMocks PaymentWebhookService service;

    private static PaymentWebhook paid(int amountThb) {
        return new PaymentWebhook("evt_1", "mock_ref_7", "successful", amountThb, NOW);
    }

    private Topup pendingTopup(Wallet w) {
        Topup t = topup(7L, w, 500, NOW.plusSeconds(900));
        when(topupRepository.findByGatewayRefForUpdate("mock_ref_7")).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    @DisplayName("TC-WH-01 จ่ายสำเร็จ → topup PAID ยอดเพิ่ม และบันทึก ledger TOPUP")
    void successful_creditsWallet() {
        Wallet w = wallet(1L, 4L, "100.00");
        Topup t = pendingTopup(w);
        when(paymentEventRepository.existsById("evt_1")).thenReturn(false);

        service.handle(paid(500), RAW);

        assertEquals(TopupStatus.PAID, t.getStatus());
        assertEquals(NOW, t.getPaidAt());
        assertEquals(new BigDecimal("600.00"), w.getBalance());

        ArgumentCaptor<WalletTransaction> txn = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txn.capture());
        assertEquals(TransactionType.TOPUP, txn.getValue().getType());
        assertEquals(RefType.TOPUP, txn.getValue().getRefType());
        assertEquals(7L, txn.getValue().getRefId());
        assertEquals(new BigDecimal("500.00"), txn.getValue().getAmount());
        assertEquals(txn.getValue(), t.getTransaction());
    }

    @Test
    @DisplayName("TC-WH-02 เก็บ event ก่อน แล้วล็อก topup และ wallet ตามลำดับ")
    void successful_order() {
        Wallet w = wallet(1L, 4L, "0.00");
        pendingTopup(w);

        service.handle(paid(500), RAW);

        InOrder order = inOrder(paymentEventRepository, topupRepository, walletRepository, walletTransactionRepository);
        order.verify(paymentEventRepository).existsById("evt_1");
        order.verify(paymentEventRepository).saveAndFlush(any(PaymentEvent.class));
        order.verify(topupRepository).findByGatewayRefForUpdate("mock_ref_7");
        order.verify(walletRepository).findByIdForUpdate(1L);
        order.verify(walletTransactionRepository).save(any());
    }

    @Test
    @DisplayName("TC-WH-03 เก็บ payload ดิบไว้ใน payment_event")
    void storesRawPayload() {
        pendingTopup(wallet(1L, 4L, "0.00"));

        service.handle(paid(500), RAW);

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).saveAndFlush(event.capture());
        assertEquals("evt_1", event.getValue().getEventId());
        assertEquals("mock_ref_7", event.getValue().getGatewayRef());
        assertEquals(500, event.getValue().getAmountThb());
        assertEquals(RAW, event.getValue().getPayload());
        assertTrue(event.getValue().isSuccessful());
    }

    @Test
    @DisplayName("TC-WH-04 eventId ซ้ำ → ข้ามทั้งหมด ไม่เติมซ้ำ")
    void duplicateEvent_ignored() {
        when(paymentEventRepository.existsById("evt_1")).thenReturn(true);

        service.handle(paid(500), RAW);

        verify(paymentEventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(topupRepository, walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-05 gatewayRef ที่ไม่รู้จัก → เก็บ event แต่ไม่ทำอะไรต่อ")
    void unknownRef() {
        when(topupRepository.findByGatewayRefForUpdate("mock_ref_7")).thenReturn(Optional.empty());

        service.handle(paid(500), RAW);

        verify(paymentEventRepository).saveAndFlush(any(PaymentEvent.class));
        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-06 จ่ายหลัง QR หมดอายุ (EXPIRED) → ยังเติมให้ (Q1 = A)")
    void expiredButPaid_stillCredits() {
        Wallet w = wallet(1L, 4L, "0.00");
        Topup t = withStatus(pendingTopup(w), TopupStatus.EXPIRED);

        service.handle(paid(500), RAW);

        assertEquals(TopupStatus.PAID, t.getStatus());
        assertEquals(new BigDecimal("500.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-WH-07 topup PAID ไปแล้ว (event ใหม่แต่รายการเดิม) → ไม่เติมซ้ำ")
    void alreadyPaid_ignored() {
        Wallet w = wallet(1L, 4L, "0.00");
        Topup t = pendingTopup(w);
        t.markPaid(NOW);                       // ยอดเป็น 500 แล้ว

        service.handle(new PaymentWebhook("evt_2", "mock_ref_7", "successful", 500, NOW), RAW);

        assertEquals(new BigDecimal("500.00"), w.getBalance());
        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-08 topup FAILED ไปแล้ว → ข้าม")
    void failedTopup_ignored() {
        Topup t = withStatus(pendingTopup(wallet(1L, 4L, "0.00")), TopupStatus.FAILED);

        service.handle(paid(500), RAW);

        assertEquals(TopupStatus.FAILED, t.getStatus());
        verifyNoInteractions(walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-09 ยอดใน webhook ไม่ตรงกับ topup → ไม่เติม สถานะคงเดิม")
    void amountMismatch_ignored() {
        Wallet w = wallet(1L, 4L, "0.00");
        Topup t = pendingTopup(w);

        service.handle(paid(1), RAW);

        assertEquals(TopupStatus.PENDING, t.getStatus());
        assertEquals(new BigDecimal("0.00"), w.getBalance());
        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-10 status failed → topup FAILED ไม่เติมเงิน")
    void failedPayment_marksFailed() {
        Wallet w = wallet(1L, 4L, "0.00");
        Topup t = pendingTopup(w);

        service.handle(new PaymentWebhook("evt_1", "mock_ref_7", "failed", 500, null), RAW);

        assertEquals(TopupStatus.FAILED, t.getStatus());
        assertNull(t.getPaidAt());
        verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-11 ไม่มี paidAt มา → ใช้เวลาตอนรับ webhook")
    void missingPaidAt_usesNow() {
        Topup t = pendingTopup(wallet(1L, 4L, "0.00"));
        Instant before = Instant.now();

        service.handle(new PaymentWebhook("evt_1", "mock_ref_7", "successful", 500, null), RAW);

        assertNotNull(t.getPaidAt());
        assertTrue(!t.getPaidAt().isBefore(before));
    }

    // =================== ผลจากการ poll (applyPolled) ===================

    @Test
    @DisplayName("TC-WH-12 poll ได้ successful → eventId = poll:ref:status เก็บ payload เป็น JSON และเติม Token")
    void applyPolled_successful() {
        Wallet w = wallet(1L, 4L, "1240.00");
        Topup t = pendingTopup(w);

        service.applyPolled(new PaymentResult("mock_ref_7", "successful", 500, NOW));

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).saveAndFlush(event.capture());
        assertEquals("poll:mock_ref_7:successful", event.getValue().getEventId());
        assertTrue(event.getValue().getPayload().contains("\"gatewayRef\":\"mock_ref_7\""), event.getValue().getPayload());
        assertEquals(TopupStatus.PAID, t.getStatus());
        assertEquals(NOW, t.getPaidAt());
        assertEquals(new BigDecimal("1740.00"), w.getBalance());
    }

    @Test
    @DisplayName("TC-WH-13 poll ซ้ำ (eventId เดิมมีแล้ว) → ไม่เติมซ้ำ")
    void applyPolled_repeated_ignored() {
        when(paymentEventRepository.existsById("poll:mock_ref_7:successful")).thenReturn(true);

        service.applyPolled(new PaymentResult("mock_ref_7", "successful", 500, NOW));

        verify(paymentEventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(topupRepository, walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-14 poll ได้ failed → topup FAILED")
    void applyPolled_failed() {
        Topup t = pendingTopup(wallet(1L, 4L, "0.00"));

        service.applyPolled(new PaymentResult("mock_ref_7", "failed", 500, null));

        assertEquals(TopupStatus.FAILED, t.getStatus());
        verifyNoInteractions(walletTransactionRepository);
    }

    @Test
    @DisplayName("TC-WH-15 poll ได้ยอดไม่ตรงกับ topup → ไม่เติม")
    void applyPolled_amountMismatch() {
        Topup t = pendingTopup(wallet(1L, 4L, "0.00"));

        service.applyPolled(new PaymentResult("mock_ref_7", "successful", 5, NOW));

        assertEquals(TopupStatus.PENDING, t.getStatus());
        verifyNoInteractions(walletTransactionRepository);
    }
}
