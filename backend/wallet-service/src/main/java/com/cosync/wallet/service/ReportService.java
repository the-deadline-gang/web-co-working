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
import com.cosync.wallet.repository.WalletTransactionRepository;
import com.cosync.wallet.repository.WalletTransactionRepository.RevenueRow;

@Service 
public class ReportService {

  
     private final WalletTransactionRepository walletTransactionRepository;

     public ReportService(WalletTransactionRepository walletTransactionRepository){
        this.walletTransactionRepository = walletTransactionRepository;
     }


    @Transactional(readOnly = true)
    public RevenueReport revenue(LocalDate from, LocalDate to) {
        ThaiDateRange.validate(from, to);
        Map<String, Map<String, BigDecimal>> byDay = new HashMap<>();      // day → (type → total)
        for (RevenueRow row : walletTransactionRepository.sumByThaiDayAndType(ThaiDateRange.startOf(from),ThaiDateRange.endOf(to))) {
            byDay.computeIfAbsent(row.getDay(), d -> new HashMap<>()).put(row.getType(), row.getTotal());
        }

        List<RevenueReport.Day> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, BigDecimal> t = byDay.getOrDefault(d.toString(), Map.of());
            days.add(new RevenueReport.Day(d,
                    sum(t, "TOPUP"),
                    sum(t, "BOOKING").negate(),       // ledger เก็บเงินออกเป็นลบ รายงานแสดงเป็นบวก
                    sum(t, "REFUND"),
                    sum(t, "LOAN_FEE").negate(),
                    Money.ZERO));                      // ⚠️ ดูหมายเหตุด้านล่าง
        }
        return new RevenueReport(from, to, days);
    }

    private static BigDecimal sum(Map<String, BigDecimal> t, String type) {
        return t.getOrDefault(type, Money.ZERO);
    }
    
}

