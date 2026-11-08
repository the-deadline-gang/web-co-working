package com.cosync.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.repository.WalletSettingRepository;

@Service 
public class TopupService {
    
    private final WalletSettingRepository walletSettingRepository;

    public TopupService( WalletSettingRepository walletSettingRepository){
        this.walletSettingRepository = walletSettingRepository;
    }

    @Transactional(readOnly = true)
    public TopupConfigResponse getConfig() {
        return TopupConfigResponse.from(walletSettingRepository.findById(1).orElseThrow());
    }
}
