package com.cosync.wallet.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWallet(Wallet wallet, Pageable pageable);

    Page<WalletTransaction> findByWalletAndType(Wallet wallet, TransactionType type, Pageable pageable);

    /** W-05: เช็กว่า Idempotency-Key นี้เคยใช้แล้วหรือยัง */
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
    interface RevenueRow {
    String getDay();          // "2026-11-14" (วันไทย)
    String getType();         // TOPUP, BOOKING, ...
    BigDecimal getTotal();
    }

    @Query(value = """
            SELECT DATE_FORMAT(CONVERT_TZ(created_at, '+00:00', '+07:00'), '%Y-%m-%d') AS day,
                type,
                SUM(amount) AS total
            FROM wallet_transaction
            WHERE created_at >= :start AND created_at < :end
            GROUP BY day, type
            """, nativeQuery = true)
    List<RevenueRow> sumByThaiDayAndType(@Param("start") Instant start, @Param("end") Instant end);
}
