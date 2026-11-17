package com.cosync.wallet.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.report.RevenueReport;
import com.cosync.wallet.dto.topup.AdminTopupResponse;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.security.RequirePermission;
import com.cosync.wallet.service.ReportService;
import com.cosync.wallet.service.TopupService;

/**
 * หน้ารายงานของ admin (ต้องมีสิทธิ์ report.view)
 * 2 endpoint นี้นับวันต่างกัน: /topups นับตามวันที่สร้าง topup (ดูงานประจำวัน รวม PENDING/EXPIRED)
 * ส่วน /reports/revenue นับตามวันที่เงินเข้าจริง (บัญชี) topup ที่สร้าง 23:58 จ่าย 00:02 จึงอยู่คนละวัน
 */
@RestController
@RequestMapping("/admin")
public class AdminReportController {

    private final TopupService topupService;
    private final ReportService reportService;

    public AdminReportController(TopupService topupService, ReportService reportService) {
        this.topupService = topupService;
        this.reportService = reportService;
    }

    /** รายการ topup ตามวันที่สร้าง (เวลาไทย) กรองสถานะได้ เรียงใหม่สุดก่อน */
    @RequirePermission("report.view")
    @GetMapping("/topups")
    public PageResponse<AdminTopupResponse> topups(
            @RequestParam(required = false) TopupStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return topupService.searchForAdmin(status, from, to, page, size);
    }

    /** ยอดรายวันตามวันที่เงินเข้า-ออกจริง (เวลาไทย) */
    @RequirePermission("report.view")
    @GetMapping("/reports/revenue")
    public RevenueReport revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenue(from, to);
    }
}
