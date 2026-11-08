package com.cosync.wallet.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.repository.WalletRepository;

@Service
public class WalletService {
     private final WalletRepository walletRepository;

     public WalletService(WalletRepository walletRepository){
        this.walletRepository = walletRepository;
     }


    @Transactional(readOnly = true)
    public WalletResponse getByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "ไม่พบกระเป๋าเงิน"));
        return WalletResponse.from(wallet);
    }



}
