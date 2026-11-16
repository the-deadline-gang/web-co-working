package com.cosync.wallet.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.common.ThaiDateRange;
import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.topup.AdminTopupResponse;
import com.cosync.wallet.dto.topup.CreateTopupRequest;
import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletSetting;
import com.cosync.wallet.payment.CreatedPayment;
import com.cosync.wallet.payment.PaymentGateway;
import com.cosync.wallet.repository.TopupRepository;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletSettingRepository;

@Service
public class TopupService {
    private static final int MAX_PAGE_SIZE = 100;

    private final WalletSettingRepository walletSettingRepository;
    private final WalletRepository walletRepository;
    private final TopupRepository topupRepository;
    private final PaymentGateway paymentGateway;

    public TopupService(WalletSettingRepository walletSettingRepository,
                        WalletRepository walletRepository,
                        TopupRepository topupRepository,
                        PaymentGateway paymentGateway) {
        this.walletSettingRepository = walletSettingRepository;
        this.walletRepository = walletRepository;
        this.topupRepository = topupRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * เป็นการดึงการตั้งค่าของ Topup
     * @return ส่งกลับแบบ TopupConfigResponse
     */
    @Transactional(readOnly = true)
    public TopupConfigResponse getConfig() {
        return TopupConfigResponse.from(walletSettingRepository.findById(WalletSetting.SINGLETON_ID).orElseThrow());
    }

    /**
     * การสร้างการจ่ายเงินโดยที่จะมีการเช็คเงื่อนไขต่างๆ
     * ลำดับการทำงานจะมีการเช็ค setting ของ wallet ก่อน ต่อด้วยการเช็คว่ายอดเงินที่ใส่มันเกินค่าที่ตั้งไว้ไหม
     * ต่อด้วยการหาว่าเจอ id wallet ของ user ไหม
     * แล้วก็ทำการสร้าง payment โดยจะไปเรียก payment ภายนอกให้สร้าง qr-code
     * จะทำการบันทึกลง Topup รายการนี้ลง DB
     * เรียก payment ภายนอกก่อน save: ถ้า payment ล้มจะไม่มีอะไรถูกบันทึก ถ้า save ล้มจะมี charge ค้างที่ payment
     * แต่ลูกค้าไม่ได้ QR จึงจ่ายไม่ได้ และ charge จะหมดอายุเอง (DB กับระบบภายนอก rollback พร้อมกันไม่ได้)
     * @param userId id ของ user
     * @param req  คำขอในรูปแบบ CreateTopupRequest
     * @return ส่งกลับแบบ TopupResponse
     */
    @Transactional
    public TopupResponse create(Long userId, CreateTopupRequest req) {
        WalletSetting setting = walletSettingRepository.findById(WalletSetting.SINGLETON_ID).orElseThrow();
        if (!setting.allowsAmount(req.amountThb())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "TOPUP_AMOUNT_OUT_OF_RANGE",
                    "เติมได้ตั้งแต่ %,d ถึง %,d บาท".formatted(setting.getMinThb(), setting.getMaxThb()));
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));

        Instant expiresAt = Instant.now().plus(setting.getQrExpireMinutes(), ChronoUnit.MINUTES);
        CreatedPayment payment = paymentGateway.createPayment(req.amountThb(), expiresAt);
        Topup saved = new Topup(wallet, req.amountThb(), setting.getRate(),
                payment.gatewayRef(), payment.qrImage(), expiresAt, req.returnToBookingId());

        topupRepository.save(saved);
        return TopupResponse.from(saved);
    }

    /**
     * เอาไว้ดึงข้อมูลการเงินของ user คนนั้น
     * โดยที่ถ้ามันเกินเวลาแล้วจะเปลี่ยนสถานะว่ามันเกินเวลาที่กำหนดแล้วโดยจะมีการเช็คก่อนว่ามันยัง Pending อยู่ไหม
     * (method นี้จึงเขียน DB ได้ ไม่ใช่ readOnly)
     * ไม่พบ หรือเป็นของคนอื่น ตอบ 404 เหมือนกัน จะได้ไม่บอกว่า id นี้มีอยู่จริง
     * @param userId id ของ user
     * @param topupId id การเติมเงิน
     * @return ส่งกลับไปแบบ TopupResponse
     */
    @Transactional
    public TopupResponse get(Long userId, Long topupId) {
        Topup topup = topupRepository.findById(topupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", "ไม่พบรายการเติมเงิน"));
        if (!topup.isOwnedBy(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", "ไม่พบรายการเติมเงิน");
        }
        if (topup.isOverdue(Instant.now())) {
            topup.markExpired();
        }
        return TopupResponse.from(topup);
    }

    /**
     * เป็นการค้นหาประวัติการเงินสำหรับ Admin
     * @param status สถานะของการเงินนั้นที่จะหา
     * @param from จากวันที่เท่าไหร่
     * @param to ถึงวันที่เท่าไหร่
     * @param page หน้าที่เท่าไหร่ (เริ่มที่ 0)
     * @param size หน้าละเท่าไหร่ (สูงสุด 100)
     * @return ตอบกลับเป็น  PageResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminTopupResponse> searchForAdmin(TopupStatus status, LocalDate from, LocalDate to,
                                                            int page, int size) {
        ThaiDateRange.validate(from, to);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Topup> result = topupRepository.searchForAdmin(status,
                ThaiDateRange.startOf(from), ThaiDateRange.endOf(to), pageable);
        return PageResponse.from(result, AdminTopupResponse::from);
    }
}
