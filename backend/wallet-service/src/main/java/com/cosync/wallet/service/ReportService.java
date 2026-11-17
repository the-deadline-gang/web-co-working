package com.cosync.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ThaiDateRange;
import com.cosync.wallet.dto.report.RevenueReport;
import com.cosync.wallet.entity.Money;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.repository.WalletTransactionRepository;
import com.cosync.wallet.repository.WalletTransactionRepository.RevenueRow;

/**
 * รายงานรายได้รายวัน (W-10) นับจาก ledger ตามวันที่เงินเข้า-ออกจริง (เวลาไทย)
 * ต่างจาก /admin/topups ที่นับตามวันที่สร้าง topup
 */
@Service
public class ReportService {

    private final WalletTransactionRepository walletTransactionRepository;

    public ReportService(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /**
     * สรุปยอดแต่ละประเภทของทุกวันตั้งแต่ from ถึง to (รวมทั้ง 2 วัน)
     * วันที่ไม่มีรายการจะได้ 0 ทุกช่อง กราฟจะได้ไม่ขาดเป็นช่วงๆ
     * @param from วันแรก (เวลาไทย)
     * @param to วันสุดท้าย (เวลาไทย) ห่างจาก from ได้ไม่เกิน 366 วัน
     * @return ยอดรายวัน แสดงเป็นบวกทุกช่อง
     */
    @Transactional(readOnly = true)
    public RevenueReport revenue(LocalDate from, LocalDate to) {
        ThaiDateRange.validate(from, to);

        Map<String, Map<String, BigDecimal>> byDay = new HashMap<>();      // วัน → (type → ยอดรวม)
        List<RevenueRow> rows = walletTransactionRepository.sumByThaiDayAndType(
                ThaiDateRange.startOf(from), ThaiDateRange.endOf(to));
        for (RevenueRow row : rows) {
            byDay.computeIfAbsent(row.getDay(), d -> new HashMap<>()).put(row.getType(), row.getTotal());
        }

        List<RevenueReport.Day> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, BigDecimal> totals = byDay.getOrDefault(d.toString(), Map.of());
            days.add(new RevenueReport.Day(d,
                    sum(totals, TransactionType.TOPUP),
                    sum(totals, TransactionType.BOOKING).negate(),    // ledger เก็บเงินออกเป็นลบ รายงานแสดงเป็นบวก
                    sum(totals, TransactionType.REFUND),
                    sum(totals, TransactionType.LOAN_FEE).negate(),
                    Money.ZERO));   // depositKept: ตาม Q7 ยังไม่มีกรณียึดมัดจำ จึงเป็น 0 เสมอ
        }
        return new RevenueReport(from, to, days);
    }

    private static BigDecimal sum(Map<String, BigDecimal> totals, TransactionType type) {
        return totals.getOrDefault(type.name(), Money.ZERO);
    }
}
