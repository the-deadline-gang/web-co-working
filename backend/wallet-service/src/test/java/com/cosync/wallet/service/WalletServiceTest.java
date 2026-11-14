package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static com.cosync.wallet.testsupport.WalletTestData.booking;
import static com.cosync.wallet.testsupport.WalletTestData.refund;
import static com.cosync.wallet.testsupport.WalletTestData.savedDebit;
import static com.cosync.wallet.testsupport.WalletTestData.setting;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.setting.WalletSettingRequest;
import com.cosync.wallet.dto.setting.WalletSettingsResponse;
import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.MoneyMoveRequest;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletTransaction;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletSettingRepository;
import com.cosync.wallet.repository.WalletTransactionRepository;
import com.cosync.wallet.testsupport.WalletTestData;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;
    @Mock WalletSettingRepository walletSettingRepository;

    @InjectMocks WalletService walletService;

    /** save() ของจริงให้ id มา ตรงนี้จำลองให้เหมือน DB */
    private void saveReturnsWithId(long id) {
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> {
            WalletTransaction t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", id);
            return t;
        });
    }

    // =================== W-04 ดูยอดและประวัติ ===================

    @Nested
    @DisplayName("W-04 ยอดเงินและประวัติ")
    class Read {

        @Test
        @DisplayName("TC-WAL-01 มี wallet → ได้ยอดและ displayId W-xxxxxx")
        void getByUserId_found() {
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(wallet(1L, 4L, "360.00")));

            WalletResponse r = walletService.getByUserId(4L);

            assertEquals(1L, r.walletId());
            assertEquals("W-000001", r.displayId());
            assertEquals(new BigDecimal("360.00"), r.balance());
        }

        @Test
        @DisplayName("TC-WAL-02 ไม่มี wallet → 404 WALLET_NOT_FOUND")
        void getByUserId_notFound() {
            when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertApiError(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", () -> walletService.getByUserId(99L));
        }

        @Test
        @DisplayName("TC-WAL-03 ประวัติไม่กรอง type → ใหม่สุดก่อน และปรับ page/size ที่ผิดให้อยู่ในช่วง")
        void transactions_allTypes_clampsPaging() {
            Wallet w = wallet(1L, 4L, "0.00");
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByWallet(eq(w), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.of(savedDebit(5L, "240", "k", "h")), inv.getArgument(1), 1));

            PageResponse<TransactionResponse> r = walletService.getTransactions(4L, null, -5, 5000);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(walletTransactionRepository).findByWallet(eq(w), pageable.capture());
            assertEquals(0, pageable.getValue().getPageNumber());
            assertEquals(100, pageable.getValue().getPageSize());
            assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
            assertEquals(1, r.items().size());
            assertEquals(5L, r.items().get(0).txnId());
            assertEquals(1, r.totalItems());
        }

        @Test
        @DisplayName("TC-WAL-04 กรอง type → ใช้ query ที่กรอง type")
        void transactions_filterByType() {
            Wallet w = wallet(1L, 4L, "0.00");
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByWalletAndType(eq(w), eq(TransactionType.TOPUP), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(2), 0));

            PageResponse<TransactionResponse> r = walletService.getTransactions(4L, TransactionType.TOPUP, 0, 0);

            assertEquals(0, r.totalItems());
            assertEquals(1, r.size());              // size 0 ถูกปรับเป็น 1
            verify(walletTransactionRepository, never()).findByWallet(any(), any());
        }

        @Test
        @DisplayName("TC-WAL-05 ดูประวัติแต่ไม่มี wallet → 404")
        void transactions_noWallet() {
            when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertApiError(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND",
                    () -> walletService.getTransactions(99L, null, 0, 20));
        }
    }

    // =================== W-06 ค่าตั้ง (admin) ===================

    @Nested
    @DisplayName("W-06 ค่าตั้ง wallet")
    class Settings {

        @Test
        @DisplayName("TC-SET-01 ดูค่าตั้ง → ได้ครบทุกช่อง")
        void getSettings() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));

            WalletSettingsResponse r = walletService.getSettings();

            assertEquals(new BigDecimal("1.0000"), r.rate());
            assertEquals(100, r.minThb());
            assertEquals(10000, r.maxThb());
            assertEquals(15, r.qrExpireMinutes());
            assertEquals("SYSTEM", r.updatedBy());
        }

        @Test
        @DisplayName("TC-SET-02 แก้ค่าตั้ง → rate ทศนิยม 4 ตำแหน่ง updatedBy = STF-xxx และ flush ทันที")
        void updateSettings_ok() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));

            WalletSettingsResponse r = walletService.updateSettings(4L,
                    new WalletSettingRequest(new BigDecimal("1"), 50, 20000, 10));

            assertEquals("1.0000", r.rate().toPlainString());
            assertEquals(50, r.minThb());
            assertEquals(20000, r.maxThb());
            assertEquals(10, r.qrExpireMinutes());
            assertEquals("STF-004", r.updatedBy());
            verify(walletSettingRepository).flush();
        }

        @Test
        @DisplayName("TC-SET-03 min มากกว่า max → 400 ไม่แตะ DB")
        void updateSettings_minGreaterThanMax() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", () -> walletService.updateSettings(4L,
                    new WalletSettingRequest(new BigDecimal("1"), 5000, 100, 15)));

            verifyNoInteractions(walletSettingRepository);
        }
    }

    // =================== W-03 สร้าง wallet ===================

    @Nested
    @DisplayName("W-03 สร้าง wallet")
    class Create {

        @Test
        @DisplayName("TC-INT-01 ยังไม่มี → สร้างใหม่ยอด 0")
        void createIfAbsent_new() {
            when(walletRepository.findByUserId(12L)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> WalletTestData.withId((Wallet) inv.getArgument(0), 7L));

            WalletResponse r = walletService.createIfAbsent(12L);

            assertEquals(7L, r.walletId());
            assertEquals(new BigDecimal("0.00"), r.balance());
        }

        @Test
        @DisplayName("TC-INT-02 มีอยู่แล้ว → คืนใบเดิม ไม่สร้างซ้ำ")
        void createIfAbsent_existing() {
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(wallet(1L, 4L, "1360.00")));

            WalletResponse r = walletService.createIfAbsent(4L);

            assertEquals(1L, r.walletId());
            assertEquals(new BigDecimal("1360.00"), r.balance());
            verify(walletRepository, never()).save(any());
        }
    }

    // =================== W-05 debit / credit ===================

    @Nested
    @DisplayName("W-05 ตัดเงินและคืนเงิน")
    class Move {

        @Test
        @DisplayName("TC-MOV-01 ตัดเงินสำเร็จ → ยอดลด บันทึก ledger พร้อม key และ hash")
        void debit_ok() {
            Wallet w = wallet(1L, 4L, "300.00");
            MoneyMoveRequest req = booking("240");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("booking-192-pay")).thenReturn(Optional.empty());
            saveReturnsWithId(10L);

            TransactionResponse r = walletService.debit(4L, "booking-192-pay", req);

            assertEquals(10L, r.txnId());
            assertEquals(new BigDecimal("-240.00"), r.amount());
            assertEquals(new BigDecimal("60.00"), r.balanceAfter());
            assertEquals(new BigDecimal("60.00"), w.getBalance());

            ArgumentCaptor<WalletTransaction> saved = ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionRepository).save(saved.capture());
            assertEquals("booking-192-pay", saved.getValue().getIdempotencyKey());
            assertEquals(req.requestHash(4L, "debit"), saved.getValue().getRequestHash());
        }

        @Test
        @DisplayName("TC-MOV-02 ล็อก wallet ก่อนเช็ก key (กันคำขอซ้ำที่มาพร้อมกัน)")
        void debit_locksBeforeCheckingKey() {
            Wallet w = wallet(1L, 4L, "300.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
            saveReturnsWithId(10L);

            walletService.debit(4L, "k", booking("10"));

            InOrder order = inOrder(walletRepository, walletTransactionRepository);
            order.verify(walletRepository).findByUserIdForUpdate(4L);
            order.verify(walletTransactionRepository).findByIdempotencyKey("k");
            order.verify(walletTransactionRepository).save(any());
        }

        @Test
        @DisplayName("TC-MOV-03 key เดิม body เดิม → ได้รายการเดิม ไม่ตัดซ้ำ")
        void debit_sameKeySameBody_returnsPrevious() {
            Wallet w = wallet(1L, 4L, "60.00");
            MoneyMoveRequest req = booking("240");
            WalletTransaction previous = savedDebit(10L, "240", "k1", req.requestHash(4L, "debit"));
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(previous));

            TransactionResponse r = walletService.debit(4L, "k1", req);

            assertEquals(10L, r.txnId());
            assertEquals(new BigDecimal("60.00"), w.getBalance());        // ยอดไม่ลดซ้ำ
            verify(walletTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-MOV-04 ส่งซ้ำตอนยอดเหลือน้อยแล้ว → ยังได้ผลเดิม ไม่ใช่ 422")
        void debit_replayWhenBalanceLow_stillSucceeds() {
            Wallet w = wallet(1L, 4L, "0.00");
            MoneyMoveRequest req = booking("240");
            WalletTransaction previous = savedDebit(10L, "240", "k1", req.requestHash(4L, "debit"));
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(previous));

            assertEquals(10L, walletService.debit(4L, "k1", req).txnId());
        }

        @Test
        @DisplayName("TC-MOV-05 key เดิม body ต่าง → 409 IDEMPOTENCY_CONFLICT")
        void debit_sameKeyDifferentBody_conflict() {
            Wallet w = wallet(1L, 4L, "1000.00");
            WalletTransaction previous = savedDebit(10L, "240", "k1", booking("240").requestHash(4L, "debit"));
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(previous));

            assertApiError(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                    () -> walletService.debit(4L, "k1", booking("999")));
            assertEquals(new BigDecimal("1000.00"), w.getBalance());
        }

        @Test
        @DisplayName("TC-MOV-06 key ที่ใช้กับ debit มาใช้กับ credit → 409 (hash ต่างทาง)")
        void credit_withDebitKey_conflict() {
            Wallet w = wallet(1L, 4L, "100.00");
            WalletTransaction previous = savedDebit(10L, "120", "k1", refund("120").requestHash(4L, "debit"));
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(previous));

            assertApiError(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                    () -> walletService.credit(4L, "k1", refund("120")));
        }

        @Test
        @DisplayName("TC-MOV-07 ยอดไม่พอ → 422 พร้อม balance และ shortfall")
        void debit_insufficient() {
            Wallet w = wallet(1L, 4L, "60.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());

            ApiException e = assertApiError(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_BALANCE",
                    () -> walletService.debit(4L, "k", booking("100")));

            assertEquals("60.00", e.getDetails().get("balance"));
            assertEquals("40.00", e.getDetails().get("shortfall"));
            assertEquals(new BigDecimal("60.00"), w.getBalance());
            verify(walletTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-MOV-08 ตัดพอดียอด → เหลือ 0.00")
        void debit_exactBalance() {
            Wallet w = wallet(1L, 4L, "240.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
            saveReturnsWithId(11L);

            assertEquals(new BigDecimal("0.00"), walletService.debit(4L, "k", booking("240")).balanceAfter());
        }

        @Test
        @DisplayName("TC-MOV-09 คืนเงิน → ยอดเพิ่ม amount เป็นบวก")
        void credit_ok() {
            Wallet w = wallet(1L, 4L, "60.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("refund-1")).thenReturn(Optional.empty());
            saveReturnsWithId(12L);

            TransactionResponse r = walletService.credit(4L, "refund-1", refund("120"));

            assertEquals(new BigDecimal("120.00"), r.amount());
            assertEquals(new BigDecimal("180.00"), r.balanceAfter());
            assertEquals(TransactionType.REFUND, r.type());
        }

        @Test
        @DisplayName("TC-MOV-10 คืนเงินตอนยอด 0 → ได้ (credit ไม่เช็กยอด)")
        void credit_fromZero() {
            Wallet w = wallet(1L, 4L, "0.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("r")).thenReturn(Optional.empty());
            saveReturnsWithId(13L);

            assertEquals(new BigDecimal("50.00"), walletService.credit(4L, "r", refund("50")).balanceAfter());
        }

        @Test
        @DisplayName("TC-MOV-11 debit ด้วย type เงินเข้า → 400 ไม่แตะ DB")
        void debit_withCreditType() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    () -> walletService.debit(4L, "k", refund("10")));
            verifyNoInteractions(walletRepository, walletTransactionRepository);
        }

        @Test
        @DisplayName("TC-MOV-12 credit ด้วย type เงินออก → 400")
        void credit_withDebitType() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    () -> walletService.credit(4L, "k", booking("10")));
            verifyNoInteractions(walletRepository, walletTransactionRepository);
        }

        @Test
        @DisplayName("TC-MOV-13 credit ด้วย TOPUP → 400 (เติมเงินต้องมาจาก webhook)")
        void credit_withTopupType() {
            MoneyMoveRequest req = new MoneyMoveRequest(new BigDecimal("10"), TransactionType.TOPUP, RefType.BOOKING, 1L, null);

            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", () -> walletService.credit(4L, "k", req));
            verifyNoInteractions(walletRepository);
        }

        @Test
        @DisplayName("TC-MOV-14 refType TOPUP → 400")
        void debit_refTypeTopup() {
            MoneyMoveRequest req = new MoneyMoveRequest(new BigDecimal("10"), TransactionType.BOOKING, RefType.TOPUP, 1L, null);

            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", () -> walletService.debit(4L, "k", req));
            verifyNoInteractions(walletRepository);
        }

        @Test
        @DisplayName("TC-MOV-15 Idempotency-Key ว่าง → 400")
        void debit_blankKey() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", () -> walletService.debit(4L, "  ", booking("10")));
            verifyNoInteractions(walletRepository);
        }

        @Test
        @DisplayName("TC-MOV-16 Idempotency-Key ยาวเกิน 100 → 400")
        void debit_keyTooLong() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    () -> walletService.debit(4L, "k".repeat(101), booking("10")));
            verifyNoInteractions(walletRepository);
        }

        @Test
        @DisplayName("TC-MOV-17 Idempotency-Key ยาว 100 พอดี → ผ่าน")
        void debit_keyExactly100() {
            String key = "k".repeat(100);
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(wallet(1L, 4L, "100.00")));
            when(walletTransactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
            saveReturnsWithId(14L);

            assertEquals(14L, walletService.debit(4L, key, booking("10")).txnId());
        }

        @Test
        @DisplayName("TC-MOV-18 ไม่มี wallet → 404 และไม่ไปเช็ก key")
        void debit_noWallet() {
            when(walletRepository.findByUserIdForUpdate(99L)).thenReturn(Optional.empty());

            assertApiError(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", () -> walletService.debit(99L, "k", booking("10")));
            verifyNoInteractions(walletTransactionRepository);
        }

        @Test
        @DisplayName("TC-MOV-19 ผลที่คืนเป็น object ที่ save() คืนมา")
        void debit_returnsSavedEntity() {
            Wallet w = wallet(1L, 4L, "100.00");
            when(walletRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(w));
            when(walletTransactionRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
            WalletTransaction fromDb = savedDebit(77L, "10", "k", "h");
            when(walletTransactionRepository.save(any(WalletTransaction.class))).thenReturn(fromDb);

            TransactionResponse r = walletService.debit(4L, "k", booking("10"));

            assertEquals(77L, r.txnId());
        }
    }
}
