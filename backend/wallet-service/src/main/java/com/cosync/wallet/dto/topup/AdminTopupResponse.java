package com.cosync.wallet.dto.topup;

import com.cosync.wallet.entity.Topup;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record AdminTopupResponse(
    @JsonUnwrapped TopupResponse topup,      // กาง field ของ TopupResponse ออกมาอยู่ระดับเดียวกัน
    String userDisplayId,
    String gatewayRef) {

public static AdminTopupResponse from(Topup t) {
    return new AdminTopupResponse(
            TopupResponse.from(t),
            "U-%06d".formatted(t.getUserId()),     // รูปแบบเดียวกับ identity (U-000004)
            t.getGatewayRef());
}
}