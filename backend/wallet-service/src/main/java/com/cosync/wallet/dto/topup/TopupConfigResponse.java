package com.cosync.wallet.dto.topup;

import java.math.BigDecimal;
import java.util.List;

import com.cosync.wallet.entity.WalletSetting;

public record TopupConfigResponse(
    BigDecimal rate, int minThb, int maxThb,
    List<Integer> quickAmounts, int qrExpireMinutes) {

public static TopupConfigResponse from(WalletSetting s) {
    return new TopupConfigResponse(
            s.getRate(), s.getMinThb(), s.getMaxThb(),
            s.quickAmounts(), s.getQrExpireMinutes());
}
}
