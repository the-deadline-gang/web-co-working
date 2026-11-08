package com.cosync.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.PageResponse;
import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.TransactionType;
import com.cosync.wallet.security.MemberOnly;
import com.cosync.wallet.service.TopupService;
import com.cosync.wallet.service.WalletService;

@RestController
@RequestMapping("/me/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TopupService topupService;

    public WalletController(WalletService walletService,TopupService topupService) {
        this.walletService = walletService;
        this.topupService = topupService;
    }

    @MemberOnly
    @GetMapping
    public WalletResponse getMyWallet(@RequestHeader("X-User-Id") Long userId) {
        return walletService.getByUserId(userId);
    }

    @MemberOnly
    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> getMyTransactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.getTransactions(userId, type, page, size);
    }


}