package com.cosync.wallet.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.topup.CreateTopupRequest;
import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletSetting;
import com.cosync.wallet.payment.CreatedPayment;
import com.cosync.wallet.payment.PaymentGateway;
import com.cosync.wallet.repository.TopupRepository;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletSettingRepository;


// review 10/11/2026 by peak
@Service 
public class TopupService {
    
    private final WalletSettingRepository walletSettingRepository;
    private final WalletRepository walletRepository;
    private final TopupRepository topupRepository;
    private final PaymentGateway paymentGateway;

    public TopupService( WalletSettingRepository walletSettingRepository,WalletRepository walletRepository,TopupRepository topupRepository,PaymentGateway paymentGateway){
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
        return TopupConfigResponse.from(walletSettingRepository.findById(1).orElseThrow());
    }

    /**
     * การสร้างการจ่ายเงินโดยที่จะมีการเช็คเงื่อนไขต่างๆ
     * @param userId id ของ user 
     * @param req  คำข้อในรูปแบบ CreateTopupRequest
     * @return ส่งกลับแบบ TopupResponse
     */
    @Transactional
    public TopupResponse create(Long userId, CreateTopupRequest req){
        WalletSetting setting = walletSettingRepository.findById(1).orElseThrow();
        if (!setting.allowsAmount(req.amountThb())) {
           throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "TOPUP_AMOUNT_OUT_OF_RANGE", "เติมได้ตั้งแต่ %,d ถึง %,d บาท".formatted(setting.getMinThb(), setting.getMaxThb()) );
        }

        Wallet wallet =  walletRepository.findByUserId(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));

        CreatedPayment payment = paymentGateway.createPayment(req.amountThb());

        Instant expiresAt = Instant.now().plus(setting.getQrExpireMinutes(), ChronoUnit.MINUTES);
        Topup saved =  new Topup(wallet, req.amountThb(), setting.getRate(), payment.gatewayRef(), payment.qrImage(), expiresAt, req.returnToBookingId());
        topupRepository.save(saved);
        return  TopupResponse.from(saved);
    }

    /**
     * เอาไว้ดึงข้อมูลการเงินของ user คนนั้น
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
}
