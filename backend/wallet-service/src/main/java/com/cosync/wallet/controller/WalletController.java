package com.cosync.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.security.MemberOnly;
import com.cosync.wallet.service.WalletService;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @MemberOnly
    @GetMapping
    public WalletResponse getMyWallet(@RequestHeader("X-User-Id") Long userId) {
        return walletService.getByUserId(userId);
    }
}