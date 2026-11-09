package com.cosync.wallet.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWallet(Wallet wallet, Pageable pageable);

    Page<WalletTransaction> findByWalletAndType(Wallet wallet, TransactionType type, Pageable pageable);

    /** W-05: เช็กว่า Idempotency-Key นี้เคยใช้แล้วหรือยัง */
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
}
