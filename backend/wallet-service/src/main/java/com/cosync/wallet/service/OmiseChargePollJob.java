package com.cosync.wallet.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cosync.wallet.dto.setting.WalletSettingRequest;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.repository.TopupRepository;

/**
 * ไล่ถาม Omise ว่า topup ที่ค้างอยู่จ่ายแล้วหรือยัง (กรณีสมาชิกปิดหน้าจอไปแล้ว ไม่มีใคร poll)
 * ทำงานเฉพาะ provider=omise ถ้าเป็น mock ไม่มีใครให้ถาม
 * ⚠️ ห้ามใส่ @Transactional เพราะต้องรอ HTTP
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "omise")
public class OmiseChargePollJob {

    private static final Logger log = LoggerFactory.getLogger(OmiseChargePollJob.class);

    /** ถามย้อนหลัง 2 เท่าของอายุ QR นานสุด เผื่อคนจ่ายนาทีสุดท้ายแล้ว Omise อัปเดตช้า */
    private static final Duration WINDOW =
            Duration.ofMinutes(WalletSettingRequest.MAX_QR_EXPIRE_MINUTES * 2L);
    private static final List<TopupStatus> OPEN = List.of(TopupStatus.PENDING, TopupStatus.EXPIRED);

    private final TopupRepository topupRepository;
    private final PaymentPoller paymentPoller;

    public OmiseChargePollJob(TopupRepository topupRepository, PaymentPoller paymentPoller) {
        this.topupRepository = topupRepository;
        this.paymentPoller = paymentPoller;
    }

    // วนทุก 15 วิ (นับจากตอนที่รอบก่อนทำเสร็จ = fixedDelay) ไล่ถาม gateway ว่ารายการที่ค้างอยู่จ่ายแล้วหรือยัง
    @Scheduled(fixedDelay = 15_000)
    public void pollOpenCharges() {
        // topup ที่ยัง PENDING หรือ EXPIRED และสร้างภายในช่วง WINDOW
        List<Topup> open = topupRepository.findByStatusInAndCreatedAtAfter(OPEN, Instant.now().minus(WINDOW));
        for (Topup topup : open) {
            try {
                paymentPoller.poll(topup);
            } catch (RuntimeException e) {
                // รายการเดียวพังต้องไม่ทำให้รายการอื่นในรอบนี้ไม่ถูกถาม
                log.warn("poll topup {} ไม่สำเร็จ: {}", topup.getId(), e.getMessage());
            }
        }
    }
}
