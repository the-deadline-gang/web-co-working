package com.cosync.wallet.dto.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import com.cosync.wallet.entity.RefType;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.WalletTransaction;

public record TransactionResponse(
        Long txnId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        RefType refType,
        Long refId,
        String note,
        Instant createdAt) {

    public static TransactionResponse from(WalletTransaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getType(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                txn.getRefType(),
                txn.getRefId(),
                txn.getNote(),
                txn.getCreatedAt());
    }
}