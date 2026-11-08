package com.cosync.wallet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.common.ApiException;
import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.entity.Wallet;
import com.cosync.wallet.entity.WalletTransaction;
import com.cosync.wallet.repository.WalletRepository;
import com.cosync.wallet.repository.WalletTransactionRepository;

@Service
public class WalletService {
     private final WalletRepository walletRepository;
     private final WalletTransactionRepository walletTransactionRepository;

     private static final int MAX_PAGE_SIZE = 100;

     public WalletService(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository){
        this.walletRepository = walletRepository;
        this.walletTransactionRepository= walletTransactionRepository ;
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



}
