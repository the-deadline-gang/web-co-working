package com.cosync.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.wallet.entity.WalletSetting;

public interface WalletSettingRepository extends JpaRepository<WalletSetting, Integer>{
    
}
