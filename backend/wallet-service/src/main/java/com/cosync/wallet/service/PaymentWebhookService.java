package com.cosync.wallet.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.dto.webhook.PaymentWebhook;
import com.cosync.wallet.entity.PaymentEvent;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.WalletTransaction;
import com.cosync.wallet.repository.PaymentEventRepository;
import com.cosync.wallet.repository.TopupRepository;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletTransactionRepository;

// review 10/11/2026 by peak
/**
 * รับผลการจ่ายเงินจาก Gateway แล้วเติม Token (F3 ขั้น 4)
 * กรณีผิดปกติส่วนใหญ่แค่ log แล้วจบ ไม่ throw เพราะถ้าตอบ error Gateway จะยิงซ้ำมาเรื่อยๆ
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentEventRepository paymentEventRepository;
    private final TopupRepository topupRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public PaymentWebhookService(PaymentEventRepository paymentEventRepository,
                                 TopupRepository topupRepository,
                                 WalletRepository walletRepository,
                                 WalletTransactionRepository walletTransactionRepository) {
        this.paymentEventRepository = paymentEventRepository;
        this.topupRepository = topupRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /**
     * เอาไว้จัดการ webhook และก็จัดการเงื่อนไขต่างๆ
     * @param event  รับมาเป็นแบบ PaymentWebhook เอาไว้เช็ค 
     * @param rawBody ข้อมูลดิบๆทั้งก้อน
     */
    @Transactional
    public void handle(PaymentWebhook event, String rawBody) {
        // 1. กันซ้ำ : event นี้เคยมาแล้ว = จบ
        if (paymentEventRepository.existsById(event.eventId())) {
            log.info("webhook {} ซ้ำ ข้าม", event.eventId());
            return;
        }
        // บันทึกทันที ถ้ามีอีกตัวมาพร้อมกันจะชน PK ตรงนี้ แล้ว rollback ทั้งก้อน
        paymentEventRepository.saveAndFlush(new PaymentEvent(
                event.eventId(), event.gatewayRef(), event.status(), event.amountThb(), rawBody));

        // 2. หา topup แล้วล็อกแถว (กัน webhook 2 ตัวจ่าย topup เดียวกัน)
        Optional<Topup> found = topupRepository.findByGatewayRefForUpdate(event.gatewayRef());
        if (found.isEmpty()) {
            log.warn("webhook {} อ้าง gatewayRef ที่ไม่รู้จัก: {}", event.eventId(), event.gatewayRef());
            return;
        }
        Topup topup = found.get();

        // 3. จ่ายไปแล้ว หรือ FAILED = ไม่ทำซ้ำ (EXPIRED ยังจ่ายได้ตาม Q1)
        if (!topup.canBePaid()) {
            log.info("webhook {} topup {} สถานะ {} แล้ว ข้าม", event.eventId(), topup.getId(), topup.getStatus());
            return;
        }

        // 4. อย่าเชื่อ body ทุกช่อง: ยอดต้องตรงกับที่สร้างไว้
        if (!topup.getAmountThb().equals(event.amountThb())) {
            log.error("webhook {} ยอดไม่ตรง topup {}: ได้ {} บาท คาดว่า {} บาท",
                    event.eventId(), topup.getId(), event.amountThb(), topup.getAmountThb());
            return;
        }

        if (!event.isSuccessful()) {
            topup.markFailed();
            log.info("webhook {} topup {} จ่ายไม่สำเร็จ", event.eventId(), topup.getId());
            return;
        }

        // 5. ล็อก wallet ด้วย กันชนกับ booking ที่กำลังตัดเงินกระเป๋าเดียวกัน (lost update)
        walletRepository.findByIdForUpdate(topup.getWallet().getId());

        // 6. เติมเงิน: ยอดเพิ่ม + แถว ledger + PAID ในทรานแซกชันเดียว
        Instant paidAt = event.paidAt() != null ? event.paidAt() : Instant.now();
        WalletTransaction txn = topup.markPaid(paidAt);
        walletTransactionRepository.save(txn);

        log.info("webhook {} เติม {} Token ให้ topup {} ยอดหลังเติม {}",
                event.eventId(), topup.getTokenAmount(), topup.getId(), txn.getBalanceAfter());
    }
}
