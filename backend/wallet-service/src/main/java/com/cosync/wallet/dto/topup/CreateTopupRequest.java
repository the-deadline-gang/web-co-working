package com.cosync.wallet.dto.topup;

import jakarta.validation.constraints.NotNull;

public record CreateTopupRequest(
    @NotNull(message = "กรุณาระบุจำนวนเงิน") Integer amountThb,
    Long returnToBookingId) {}