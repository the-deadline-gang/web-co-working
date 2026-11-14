package com.cosync.wallet.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReport(LocalDate from, LocalDate to, List<Day> days) {
    public record Day(LocalDate date, BigDecimal topup, BigDecimal booking, BigDecimal refund,
                      BigDecimal loanFee, BigDecimal depositKept) {}
}
