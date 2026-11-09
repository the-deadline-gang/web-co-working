package com.cosync.wallet.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.setting.WalletSettingRequest;
import com.cosync.wallet.dto.setting.WalletSettingsResponse;
import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.MoneyMoveRequest;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletSetting;
import com.cosync.wallet.entity.WalletTransaction;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletSettingRepository;
import com.cosync.wallet.repository.WalletTransactionRepository;

@Service
public class WalletService {
     private final WalletRepository walletRepository;
     private final WalletTransactionRepository walletTransactionRepository;
     private final WalletSettingRepository walletSettingRepository;

     private static final int MAX_PAGE_SIZE = 100;

     public WalletService(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository,WalletSettingRepository walletSettingRepository){
        this.walletRepository = walletRepository;
        this.walletTransactionRepository= walletTransactionRepository ;
        this.walletSettingRepository =walletSettingRepository;
     }


    @Transactional(readOnly = true)
    public WalletResponse getByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(Long userId, TransactionType type, int page, int size) {
        Wallet wallet = findWallet(userId);

        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.clamp(size, 1, MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));     // ใหม่สุดก่อน

        
        Page<WalletTransaction> result = (type == null)
            ? walletTransactionRepository.findByWallet(wallet, pageable)
            : walletTransactionRepository.findByWalletAndType(wallet, type, pageable);

        return  PageResponse.from(result, TransactionResponse::from);
    }

    private Wallet findWallet(Long userId){
        return walletRepository.findByUserId(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));
    }

   @Transactional(readOnly = true)
    public WalletSettingsResponse getSettings() {
        WalletSetting setting = walletSettingRepository.findById(WalletSetting.SINGLETON_ID).orElseThrow(); 
        return WalletSettingsResponse.from(setting);                                                        
    }


    @Transactional
    public WalletSettingsResponse updateSettings(Long staffId, WalletSettingRequest req) {
     
        if (req.minThb() > req.maxThb()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "ยอดเติมขั้นต่ำต้องไม่มากกว่ายอดเติมสูงสุด");
        }

      
        WalletSetting setting = walletSettingRepository.findById(WalletSetting.SINGLETON_ID).orElseThrow();

        setting.update(req.rate().setScale(4), req.minThb(), req.maxThb(), req.qrExpireMinutes(),
                "STF-%03d".formatted(staffId));
        walletSettingRepository.flush();          // ส่ง UPDATE ทันที @UpdateTimestamp จะใส่เวลาใหม่ให้



        return WalletSettingsResponse.from(setting);
    }
    /**
     * เป็นการสร้าง wallet ใหม่ถ้ามีแล้วก็จะไม่สร้างใหม่
     * @param userId id ของ user คนนั้น
     * @return ส่งกลับไปเป็นแบบ  WalletResponse 
     */
    @Transactional
    public WalletResponse createIfAbsent(Long userId ){
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet( () -> walletRepository.save(new Wallet(userId) ) );
        return WalletResponse.from(wallet);
    }


    /**
     * เอาไว้หักเงิน 
     * @param userId id ของ user
     * @param key Idempotency-Key ที่จะเอามาเป็นตัวเช็คว่่าเคยทำรายการนี้ไปรึยังจะไม่ได้ไม่ทำซ้ำ
     * @param req รายการคำขอในการเปลี่ยนแปลงเงิน
     * @return  ส่งกลับไปเป็น TransactionResponse
     */
    @Transactional
    public TransactionResponse debit(Long userId, String key, MoneyMoveRequest req) {
        return move(userId, key, req, false);
    }



    /**
     * เอาไว้เพิ่มเงิน 
     * @param userId id ของ user
     * @param key Idempotency-Key ที่จะเอามาเป็นตัวเช็คว่่าเคยทำรายการนี้ไปรึยังจะไม่ได้ไม่ทำซ้ำ
     * @param req รายการคำขอในการเปลี่ยนแปลงเงิน
     * @return  ส่งกลับไปเป็น TransactionResponse
     */
    @Transactional
    public TransactionResponse credit(Long userId, String key, MoneyMoveRequest req) {
        return move(userId, key, req, true);
    }

    
    /**
     * ตัดเงินหรือคืนเงิน โดยที่กระบวนการในนี้จะมีการ lock row ของ user คนนั้นเพื่อเอาค่าไปเข็คและค่่อยทำการหัก
     * @param userId id ของ user คนนั้น
     * @param key  Idempotency-Key ที่จะเอามาเป็นตัวเช็คว่่าเคยทำรายการนี้ไปรึยังจะไม่ได้ไม่ทำซ้ำ
     * @param req รายการคำขอในการเปลี่ยนแปลงเงิน
     * @param isCredit ส่งมาเป็น boolean ว่าเป็น เงินเข้าหรือเงินออก
     * @return ส่งกลับไปเป็น TransactionResponse
     */
    private TransactionResponse move(Long userId, String key, MoneyMoveRequest req, boolean isCredit) {

        if (key.isBlank() || key.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Idempotency-Key ต้องยาว 1–100 ตัวอักษร");
        }

        // type ต้องตรงกับทาง: debit รับแต่เงินออก, credit รับแต่เงินเข้า
        // TOPUP ห้ามใช้ทางนี้ เพราะการเติมเงินต้องมาจาก webhook เท่านั้น
        if (req.type().isCredit() != isCredit || req.type() == TransactionType.TOPUP) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "type " + req.type() + " ใช้กับ " + (isCredit ? "credit" : "debit") + " ไม่ได้");
        }

        // refType ใน spec มีแค่ BOOKING กับ LOAN (TOPUP เป็นของ webhook)
        if (req.refType() == RefType.TOPUP) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "refType ต้องเป็น BOOKING หรือ LOAN");
        }

        // ลายนิ้วมือของคำขอนี้ ไว้เทียบกับคำขอเดิมที่ใช้ key เดียวกัน
        String hash = req.requestHash(userId, isCredit ? "credit" : "debit");


        // ---------- ขั้น 1: ล็อก wallet ก่อนทำอะไรทั้งนั้น ----------
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));
        // ---------- ขั้น 2: key นี้เคยใช้แล้วหรือยัง ----------
        Optional<WalletTransaction> existing = walletTransactionRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            WalletTransaction previous = existing.get();
            if (previous.matchesRequest(hash)) {
                return TransactionResponse.from(previous);
            }
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                    "Idempotency-Key นี้ถูกใช้กับคำขออื่นแล้ว");
        }
        // ---------- ขั้น 3: ตัดเงินต้องมียอดพอ (คืนเงินข้ามขั้นนี้) ----------
        // เช็กหลังล็อก ยอดที่เห็นจึงเป็นยอดล่าสุดจริง ไม่มีใครแก้แทรกได้
        if (!isCredit && !wallet.hasEnough(req.amount())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_BALANCE", "ยอด Wallet ไม่พอ",
                    Map.of(
                            "balance", wallet.getBalance().toPlainString(),               // ยอดที่มี เช่น 150.00
                            "shortfall", wallet.shortfallFor(req.amount()).toPlainString() // ขาดอีกเท่าไหร่ เช่น 90.00
                    ));
        }
        // ---------- ขั้น 4: แก้ยอด + สร้างแถว ledger ----------
        WalletTransaction txn = isCredit
                ? wallet.credit(req.type(), req.amount(), req.refType(), req.refId(), req.note())
                : wallet.debit(req.type(), req.amount(), req.refType(), req.refId(), req.note());

        // ผูก key กับ hash ไว้กับแถวนี้ ครั้งหน้าที่ key เดิมมา ขั้น 2 จะหาเจอ
        txn.withIdempotency(key, hash);

        // save แถว ledger ส่วน balance ของ wallet ไม่ต้อง save เอง (dirty checking จะ UPDATE ให้ตอน commit)
        WalletTransaction saved = walletTransactionRepository.save(txn);

        return TransactionResponse.from(saved);
    }
}
