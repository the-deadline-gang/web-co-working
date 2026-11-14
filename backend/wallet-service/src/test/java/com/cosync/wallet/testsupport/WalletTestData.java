package com.cosync.wallet.testsupport;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.wallet.dto.wallet.MoneyMoveRequest;
import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletSetting;
import com.cosync.wallet.entity.WalletTransaction;

/**
 * ของปลอมสำหรับ unit test ของ wallet
 * entity มี constructor แบบ protected และไม่มี setter (ตั้งใจไว้) จึงใช้ ReflectionTestUtils ใส่ id และเวลาแทน DB
 */
public final class WalletTestData {

    public static final Instant NOW = Instant.parse("2026-11-14T05:00:00Z");   // 12:00 เวลาไทย

    private WalletTestData() {}

    public static <T> T withId(T entity, Object id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static Wallet wallet(long id, long userId, String balance) {
        Wallet wallet = new Wallet(userId);
        withId(wallet, id);
        ReflectionTestUtils.setField(wallet, "balance", new BigDecimal(balance));
        return wallet;
    }

    /** แถวค่าตั้ง (แถวเดียว id = 1) ที่ DB seed ไว้ */
    public static WalletSetting setting(String rate, int minThb, int maxThb, int qrExpireMinutes) {
        WalletSetting setting = BeanUtils.instantiateClass(WalletSetting.class);
        ReflectionTestUtils.setField(setting, "id", WalletSetting.SINGLETON_ID);
        setting.update(new BigDecimal(rate), minThb, maxThb, qrExpireMinutes, "SYSTEM");
        return setting;
    }

    /** topup ที่ save แล้ว (มี id และ createdAt) สถานะ PENDING */
    public static Topup topup(long id, Wallet wallet, int amountThb, Instant expiresAt) {
        Topup topup = new Topup(wallet, amountThb, new BigDecimal("1.0000"), "mock_ref_" + id,
                "data:image/png;base64,QR", expiresAt, null);
        withId(topup, id);
        ReflectionTestUtils.setField(topup, "createdAt", NOW);
        return topup;
    }

    public static Topup withStatus(Topup topup, TopupStatus status) {
        ReflectionTestUtils.setField(topup, "status", status);
        return topup;
    }

    /** แถว ledger ที่ save แล้ว สร้างผ่าน Wallet.debit() เหมือนโค้ดจริง */
    public static WalletTransaction savedDebit(long txnId, String amount, String key, String hash) {
        Wallet source = wallet(99L, 99L, "100000.00");
        WalletTransaction txn = source.debit(TransactionType.BOOKING, new BigDecimal(amount),
                RefType.BOOKING, 192L, "B-ROOM-02");
        txn.withIdempotency(key, hash);
        withId(txn, txnId);
        ReflectionTestUtils.setField(txn, "createdAt", NOW);
        return txn;
    }

    public static MoneyMoveRequest booking(String amount) {
        return new MoneyMoveRequest(new BigDecimal(amount), TransactionType.BOOKING, RefType.BOOKING, 192L, "B-ROOM-02");
    }

    public static MoneyMoveRequest refund(String amount) {
        return new MoneyMoveRequest(new BigDecimal(amount), TransactionType.REFUND, RefType.BOOKING, 192L, "ยกเลิก");
    }
}
