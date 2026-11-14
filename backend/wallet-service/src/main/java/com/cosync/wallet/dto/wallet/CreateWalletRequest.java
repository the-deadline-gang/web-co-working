package com.cosync.wallet.dto.wallet;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(@NotNull(message = "กรุณาระบุ userId") Long userId) {}
