package com.cosync.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.setting.WalletSettingRequest;
import com.cosync.wallet.dto.setting.WalletSettingsResponse;
import com.cosync.wallet.security.RequirePermission;
import com.cosync.wallet.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/wallet-settings")
public class AdminWalletSettingController {

    private final WalletService walletService;

    public AdminWalletSettingController(WalletService walletService){
        this.walletService = walletService;
    }

    @GetMapping()         
    @RequirePermission("setting.manage")
    public WalletSettingsResponse getConfig() {
        return walletService.getSettings();
    }

    @PutMapping
    @RequirePermission("setting.manage")
    public WalletSettingsResponse update(@RequestHeader("X-User-Id") Long staffId,
                                        @Valid @RequestBody WalletSettingRequest req) {
        return walletService.updateSettings(staffId, req);
    }
}
