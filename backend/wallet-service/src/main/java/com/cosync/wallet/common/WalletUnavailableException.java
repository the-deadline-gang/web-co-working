package com.cosync.wallet.common;

public class WalletUnavailableException extends RuntimeException {
    public WalletUnavailableException() {
        super("ระบบกระเป๋าเงินไม่พร้อมใช้งาน กรุณาลองใหม่อีกครั้ง");
    }
}
