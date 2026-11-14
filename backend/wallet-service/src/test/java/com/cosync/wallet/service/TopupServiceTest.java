package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static com.cosync.wallet.testsupport.WalletTestData.NOW;
import static com.cosync.wallet.testsupport.WalletTestData.setting;
import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static com.cosync.wallet.testsupport.WalletTestData.withStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.topup.AdminTopupResponse;
import com.cosync.wallet.dto.topup.CreateTopupRequest;
import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.payment.CreatedPayment;
import com.cosync.wallet.payment.PaymentGateway;
import com.cosync.wallet.repository.TopupRepository;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletSettingRepository;

@ExtendWith(MockitoExtension.class)
class TopupServiceTest {

    @Mock WalletSettingRepository walletSettingRepository;
    @Mock WalletRepository walletRepository;
    @Mock TopupRepository topupRepository;
    @Mock PaymentGateway paymentGateway;

    @InjectMocks TopupService topupService;

    /** save() ของจริงใส่ id และ createdAt ให้ (IDENTITY + @CreationTimestamp) ตรงนี้ทำแทน */
    private void saveAssignsId(long id) {
        when(topupRepository.save(any(Topup.class))).thenAnswer(inv -> {
            Topup t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", id);
            ReflectionTestUtils.setField(t, "createdAt", NOW);
            return t;
        });
    }

    @Test
    @DisplayName("TC-TOP-01 ดู config → ปุ่มลัดเฉพาะยอดที่อยู่ในช่วง min–max")
    void config_filtersQuickAmounts() {
        when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 150, 1000, 15)));

        TopupConfigResponse r = topupService.getConfig();

        assertEquals(List.of(200, 500, 1000), r.quickAmounts());
        assertEquals(150, r.minThb());
        assertEquals(1000, r.maxThb());
        assertEquals(15, r.qrExpireMinutes());
    }

    @Nested
    @DisplayName("POST /topups สร้างรายการเติมเงิน")
    class Create {

        @Test
        @DisplayName("TC-TOP-02 ยอดอยู่ในช่วง → ได้ PENDING พร้อม QR และหมดอายุตามค่าตั้ง")
        void create_ok() {
            Wallet w = wallet(1L, 4L, "0.00");
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(w));
            when(paymentGateway.createPayment(eq(500), any(Instant.class))).thenReturn(new CreatedPayment("mock_abc", "data:image/png;base64,QQ"));
            saveAssignsId(7L);

            Instant before = Instant.now();
            TopupResponse r = topupService.create(4L, new CreateTopupRequest(500, 192L));

            assertEquals(7L, r.topupId());
            assertEquals("PAY-20261114-0007", r.displayId());
            assertEquals(TopupStatus.PENDING, r.status());
            assertEquals(new BigDecimal("500.00"), r.tokenAmount());
            assertEquals("data:image/png;base64,QQ", r.qrImage());
            assertEquals(192L, r.returnToBookingId());
            assertNull(r.paidAt());
            assertNull(r.balanceAfter());
            Duration ttl = Duration.between(before, r.expiresAt());
            assertTrue(ttl.compareTo(Duration.ofMinutes(15)) >= 0 && ttl.compareTo(Duration.ofMinutes(16)) < 0,
                    "expiresAt ควรอยู่ประมาณ 15 นาทีจากตอนนี้");

            ArgumentCaptor<Instant> sentExpiry = ArgumentCaptor.forClass(Instant.class);
            verify(paymentGateway).createPayment(eq(500), sentExpiry.capture());
            assertEquals(r.expiresAt(), sentExpiry.getValue(), "ส่งเวลาหมดอายุเดียวกับ topup ไปให้ gateway");

            ArgumentCaptor<Topup> saved = ArgumentCaptor.forClass(Topup.class);
            verify(topupRepository).save(saved.capture());
            assertEquals("mock_abc", saved.getValue().getGatewayRef());
            assertEquals(4L, saved.getValue().getUserId());
            assertEquals(new BigDecimal("1.0000"), saved.getValue().getRate());
        }

        @Test
        @DisplayName("TC-TOP-03 rate ไม่ลงตัว → Token ปัดลง 2 ตำแหน่ง (101 × 1.2345 = 124.68)")
        void create_roundsTokensDown() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.2345", 100, 10000, 15)));
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(wallet(1L, 4L, "0.00")));
            when(paymentGateway.createPayment(eq(101), any(Instant.class))).thenReturn(new CreatedPayment("mock_x", "qr"));
            saveAssignsId(8L);

            assertEquals(new BigDecimal("124.68"), topupService.create(4L, new CreateTopupRequest(101, null)).tokenAmount());
        }

        @Test
        @DisplayName("TC-TOP-04 ยอดเท่ากับ min และ max พอดี → ผ่าน")
        void create_boundaries() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));
            when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(wallet(1L, 4L, "0.00")));
            when(paymentGateway.createPayment(anyInt(), any(Instant.class))).thenReturn(new CreatedPayment("mock_x", "qr"));
            saveAssignsId(9L);

            assertEquals(100, topupService.create(4L, new CreateTopupRequest(100, null)).amountThb());
            assertEquals(10000, topupService.create(4L, new CreateTopupRequest(10000, null)).amountThb());
        }

        @Test
        @DisplayName("TC-TOP-05 ต่ำกว่า min → 422 ข้อความบอกช่วง และไม่เรียก gateway")
        void create_belowMin() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));

            var e = assertApiError(HttpStatus.UNPROCESSABLE_CONTENT, "TOPUP_AMOUNT_OUT_OF_RANGE",
                    () -> topupService.create(4L, new CreateTopupRequest(99, null)));

            assertTrue(e.getMessage().contains("100") && e.getMessage().contains("10,000"), e.getMessage());
            verifyNoInteractions(paymentGateway, walletRepository, topupRepository);
        }

        @Test
        @DisplayName("TC-TOP-06 เกิน max → 422")
        void create_aboveMax() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));

            assertApiError(HttpStatus.UNPROCESSABLE_CONTENT, "TOPUP_AMOUNT_OUT_OF_RANGE",
                    () -> topupService.create(4L, new CreateTopupRequest(10001, null)));
            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("TC-TOP-07 ไม่มี wallet → 404 และไม่สร้าง QR")
        void create_noWallet() {
            when(walletSettingRepository.findById(1)).thenReturn(Optional.of(setting("1.0000", 100, 10000, 15)));
            when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertApiError(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND",
                    () -> topupService.create(99L, new CreateTopupRequest(500, null)));
            verifyNoInteractions(paymentGateway, topupRepository);
        }
    }

    @Nested
    @DisplayName("GET /topups/{id} ดูสถานะ")
    class Get {

        @Test
        @DisplayName("TC-TOP-08 เจ้าของดูรายการที่ยังไม่หมดอายุ → PENDING มี QR")
        void get_ownerPending() {
            Topup t = topup(7L, wallet(1L, 4L, "0.00"), 500, Instant.now().plusSeconds(600));
            when(topupRepository.findById(7L)).thenReturn(Optional.of(t));

            TopupResponse r = topupService.get(4L, 7L);

            assertEquals(TopupStatus.PENDING, r.status());
            assertNotNull(r.qrImage());
        }

        @Test
        @DisplayName("TC-TOP-09 คนอื่นดู → 404 (ไม่บอกว่ามีอยู่จริง)")
        void get_otherUser() {
            Topup t = topup(7L, wallet(1L, 4L, "0.00"), 500, Instant.now().plusSeconds(600));
            when(topupRepository.findById(7L)).thenReturn(Optional.of(t));

            assertApiError(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", () -> topupService.get(5L, 7L));
        }

        @Test
        @DisplayName("TC-TOP-10 ไม่มีรายการ → 404")
        void get_notFound() {
            when(topupRepository.findById(404L)).thenReturn(Optional.empty());

            assertApiError(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", () -> topupService.get(4L, 404L));
        }

        @Test
        @DisplayName("TC-TOP-11 เลยเวลาแล้วแต่ job ยังไม่มา → เปลี่ยนเป็น EXPIRED ทันที ไม่ส่ง QR")
        void get_overdueBecomesExpired() {
            Topup t = topup(7L, wallet(1L, 4L, "0.00"), 500, Instant.now().minusSeconds(1));
            when(topupRepository.findById(7L)).thenReturn(Optional.of(t));

            TopupResponse r = topupService.get(4L, 7L);

            assertEquals(TopupStatus.EXPIRED, r.status());
            assertEquals(TopupStatus.EXPIRED, t.getStatus());
            assertNull(r.qrImage());
        }

        @Test
        @DisplayName("TC-TOP-12 จ่ายแล้ว → PAID พร้อม paidAt และยอดหลังเติม")
        void get_paid() {
            Wallet w = wallet(1L, 4L, "100.00");
            Topup t = topup(7L, w, 500, Instant.now().plusSeconds(600));
            t.markPaid(NOW);
            when(topupRepository.findById(7L)).thenReturn(Optional.of(t));

            TopupResponse r = topupService.get(4L, 7L);

            assertEquals(TopupStatus.PAID, r.status());
            assertEquals(NOW, r.paidAt());
            assertEquals(new BigDecimal("600.00"), r.balanceAfter());
            assertNull(r.qrImage());
        }
    }

    @Nested
    @DisplayName("GET /admin/topups")
    class Admin {

        @Test
        @DisplayName("TC-ADM-01 ช่วงวันไทย 1–30 พ.ย. → ส่งเวลา UTC ที่ถูกต้องไป query และมี userDisplayId")
        void search_ok() {
            Topup t = withStatus(topup(7L, wallet(1L, 4L, "0.00"), 500, NOW), TopupStatus.EXPIRED);
            when(topupRepository.searchForAdmin(eq(TopupStatus.EXPIRED), any(), any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.of(t), inv.getArgument(3), 1));

            PageResponse<AdminTopupResponse> r = topupService.searchForAdmin(TopupStatus.EXPIRED,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 0, 500);

            ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(topupRepository).searchForAdmin(eq(TopupStatus.EXPIRED), start.capture(), end.capture(), pageable.capture());
            assertEquals(Instant.parse("2026-10-31T17:00:00Z"), start.getValue());
            assertEquals(Instant.parse("2026-11-30T17:00:00Z"), end.getValue());
            assertEquals(100, pageable.getValue().getPageSize());

            AdminTopupResponse row = r.items().get(0);
            assertEquals("U-000004", row.userDisplayId());
            assertEquals("mock_ref_7", row.gatewayRef());
            assertEquals(TopupStatus.EXPIRED, row.topup().status());
        }

        @Test
        @DisplayName("TC-ADM-02 from อยู่หลัง to → 400 ไม่ query")
        void search_invalidRange() {
            assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", () -> topupService.searchForAdmin(null,
                    LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), 0, 20));
            verify(topupRepository, never()).searchForAdmin(any(), any(), any(), any());
        }
    }
}
