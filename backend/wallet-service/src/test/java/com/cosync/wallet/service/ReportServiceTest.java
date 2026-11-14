package com.cosync.wallet.service;

import static com.cosync.wallet.testsupport.ApiErrors.assertApiError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.cosync.wallet.dto.report.RevenueReport;
import com.cosync.wallet.repository.WalletTransactionRepository;
import com.cosync.wallet.repository.WalletTransactionRepository.RevenueRow;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    /** แถวที่ native query คืนมา (day, type, total) */
    record Row(String getDay, String getType, BigDecimal getTotal) implements RevenueRow {}

    private static Row row(String day, String type, String total) {
        return new Row(day, type, new BigDecimal(total));
    }

    @Mock WalletTransactionRepository walletTransactionRepository;

    @InjectMocks ReportService reportService;

    @Test
    @DisplayName("TC-REP-01 ส่งขอบวันไทยเป็น UTC (00:00 ไทย = 17:00 UTC วันก่อน) ไป query")
    void queriesThaiDayBoundaries() {
        when(walletTransactionRepository.sumByThaiDayAndType(any(), any())).thenReturn(List.of());

        reportService.revenue(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 14));

        verify(walletTransactionRepository).sumByThaiDayAndType(
                Instant.parse("2026-11-13T17:00:00Z"), Instant.parse("2026-11-14T17:00:00Z"));
    }

    @Test
    @DisplayName("TC-REP-02 มีครบทุกวันในช่วง วันที่ไม่มีรายการเป็น 0.00")
    void fillsEveryDay() {
        when(walletTransactionRepository.sumByThaiDayAndType(any(), any()))
                .thenReturn(List.of(row("2026-11-13", "TOPUP", "500.00")));

        RevenueReport r = reportService.revenue(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14));

        assertEquals(3, r.days().size());
        assertEquals(LocalDate.of(2026, 11, 12), r.days().get(0).date());
        assertEquals(new BigDecimal("0.00"), r.days().get(0).topup());
        assertEquals(new BigDecimal("500.00"), r.days().get(1).topup());
        assertEquals(new BigDecimal("0.00"), r.days().get(2).booking());
        assertEquals(LocalDate.of(2026, 11, 12), r.from());
        assertEquals(LocalDate.of(2026, 11, 14), r.to());
    }

    @Test
    @DisplayName("TC-REP-03 เงินออก (BOOKING, LOAN_FEE) ใน ledger เป็นลบ → รายงานแสดงเป็นบวก")
    void negatesOutgoing() {
        when(walletTransactionRepository.sumByThaiDayAndType(any(), any())).thenReturn(List.of(
                row("2026-11-14", "TOPUP", "1500.00"),
                row("2026-11-14", "BOOKING", "-240.00"),
                row("2026-11-14", "REFUND", "120.00"),
                row("2026-11-14", "LOAN_FEE", "-30.00"),
                row("2026-11-14", "DEPOSIT_HOLD", "-500.00")));

        RevenueReport.Day d = reportService.revenue(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 14)).days().get(0);

        assertEquals(new BigDecimal("1500.00"), d.topup());
        assertEquals(new BigDecimal("240.00"), d.booking());
        assertEquals(new BigDecimal("120.00"), d.refund());
        assertEquals(new BigDecimal("30.00"), d.loanFee());
        assertEquals(new BigDecimal("0.00"), d.depositKept());     // Q7: ยังไม่มีข้อมูลมัดจำที่ยึด
    }

    @Test
    @DisplayName("TC-REP-04 from อยู่หลัง to → 400 ไม่ query")
    void invalidRange() {
        assertApiError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                () -> reportService.revenue(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 1)));
        verifyNoInteractions(walletTransactionRepository);
    }
}
