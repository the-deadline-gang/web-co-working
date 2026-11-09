package com.cosync.identity.common;

/**
 * wallet-service ไม่ตอบหรือตอบ error ตอนสร้าง wallet → 503 WALLET_UNAVAILABLE
 *
 * ตั้งใจไม่ extends ApiException: verifyOtp() มี @Transactional(noRollbackFor = ApiException.class)
 * ถ้าเป็นลูกของ ApiException จะไม่ rollback แล้ว email จะถูกยืนยันทั้งที่ไม่มี wallet
 */
public class WalletUnavailableException extends RuntimeException {

    public WalletUnavailableException() {
        super("ระบบกระเป๋าเงินไม่พร้อมใช้งาน กรุณากดยืนยันอีกครั้ง");
    }
}
