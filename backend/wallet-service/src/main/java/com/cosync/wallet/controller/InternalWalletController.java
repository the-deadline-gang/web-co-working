package com.cosync.wallet.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.transaction.TransactionResponse;
import com.cosync.wallet.dto.wallet.CreateWalletRequest;
import com.cosync.wallet.dto.wallet.MoneyMoveRequest;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.service.WalletService;

import jakarta.validation.Valid;
// review 10/11/2026 by peak
@RestController
@RequestMapping("/internal/wallets")
public class InternalWalletController {

    private final WalletService walletService;

    public InternalWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest req) {
        return walletService.createIfAbsent(req.userId());
    }

    @PostMapping("/{userId}/debit")
    public TransactionResponse debit(@PathVariable Long userId,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody MoneyMoveRequest req) {
        return walletService.debit(userId, idempotencyKey, req);
    }

    @PostMapping("/{userId}/credit")
    public TransactionResponse credit(@PathVariable Long userId,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody MoneyMoveRequest req) {
        return walletService.credit(userId, idempotencyKey, req);
    }
}

