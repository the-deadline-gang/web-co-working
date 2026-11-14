package com.cosync.wallet.dto.wallet;

import com.cosync.wallet.entity.Wallet;

import java.math.BigDecimal;

public record WalletResponse(
        Long walletId,
        String displayId,
        BigDecimal balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                "W-%06d".formatted(wallet.getId()),   // 18 → W-000018
                wallet.getBalance());
    }
}