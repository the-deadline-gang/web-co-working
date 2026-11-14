package com.cosync.wallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.payment.PaymentGateway;
import com.cosync.wallet.repository.TopupRepository;

/**
 * ถาม payment gateway ว่ารายการที่ค้างอยู่จ่ายแล้วหรือยัง
 * ⚠️ ห้ามใส่ @Transactional: ไม่ถือ DB connection ไว้ระหว่างรอ HTTP
 */
@Service
public class PaymentPoller {

    private static final Logger log = LoggerFactory.getLogger(PaymentPoller.class);

    private final TopupRepository topupRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentPoller(TopupRepository topupRepository, PaymentGateway paymentGateway,
                         PaymentWebhookService paymentWebhookService) {
        this.topupRepository = topupRepository;
        this.paymentGateway = paymentGateway;
        this.paymentWebhookService = paymentWebhookService;
    }

    /**
     * ถาม gateway ว่า topup นี้จ่ายแล้วหรือยัง ใช้ก่อนตอบ GET /topups/{id}
     * ถ้าหาไม่เจอหรือไม่ใช่ของ user นี้ ให้ข้ามเฉยๆ แล้วให้ TopupService.get ตอบ 404 เอง
     * @param userId  id ของสมาชิกที่เรียก (จาก X-User-Id)
     * @param topupId id ของรายการเติมเงิน
     */
    public void pollFor(Long userId, Long topupId) {
        topupRepository.findById(topupId)
                .filter(topup -> topup.isOwnedBy(userId))
                .ifPresent(this::poll);
    }
    /**
     * ถาม gateway ว่า topup รายการนี้จ่ายแล้วหรือยัง ถ้าได้ผลแล้ว (successful / failed) ให้บันทึกผลผ่าน applyPolled
     * ถ้า PAID / FAILED ไปแล้วจะไม่ถามซ้ำ
     * ใช้ได้ทั้งจาก pollFor (สมาชิกเปิดหน้าจอ) และ OmiseChargePollJob (job ทุก 15 วิ)
     * @param topup รายการเติมเงินที่จะถาม (หนึ่งรายการ)
     */
    public void poll(Topup topup) {
        if (!topup.canBePaid()) {
            return;                                    // PAID / FAILED แล้ว ไม่ต้องถาม
        }
        paymentGateway.fetchStatus(topup.getGatewayRef()).ifPresent(result -> {
            try {
                paymentWebhookService.applyPolled(result);
            } catch (DataIntegrityViolationException duplicate) {
                // อีกทาง (poll รอบอื่น / webhook) บันทึก event เดียวกันไปพร้อมกันแล้ว PK กันไว้ให้
                log.info("ผลของ {} ถูกบันทึกไปแล้ว ข้าม", topup.getGatewayRef());
            }
        });
    }
}