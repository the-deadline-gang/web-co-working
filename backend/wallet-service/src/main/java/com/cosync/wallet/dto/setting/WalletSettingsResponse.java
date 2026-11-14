package com.cosync.wallet.dto.setting;

import java.math.BigDecimal;
import java.time.Instant;

import com.cosync.wallet.entity.WalletSetting;

public record WalletSettingsResponse (
    BigDecimal rate,
    Integer minThb,
    Integer maxThb,
    Integer qrExpireMinutes,
    String updatedBy,
    Instant updatedAt
){
    public static WalletSettingsResponse from(WalletSetting s) {
        return new WalletSettingsResponse(
            s.getRate(),
            s.getMinThb(),
            s.getMaxThb(),
            s.getQrExpireMinutes(),
            s.getUpdatedBy(),
            s.getUpdatedAt());
    }


}
