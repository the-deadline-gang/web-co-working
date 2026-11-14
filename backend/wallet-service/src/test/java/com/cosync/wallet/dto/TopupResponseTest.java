package com.cosync.wallet.dto;

import static com.cosync.wallet.testsupport.WalletTestData.topup;
import static com.cosync.wallet.testsupport.WalletTestData.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.wallet.dto.topup.AdminTopupResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.dto.wallet.WalletResponse;
import com.cosync.wallet.entity.Topup;

class TopupResponseTest {

    @Test
    @DisplayName("TC-DTO-01 displayId ใช้วันที่ไทย (17:30 UTC = 00:30 วันถัดไปของไทย)")
    void displayId_usesThaiDate() {
        Topup t = topup(7L, wallet(1L, 4L, "0.00"), 500, Instant.parse("2026-11-08T18:00:00Z"));
        ReflectionTestUtils.setField(t, "createdAt", Instant.parse("2026-11-08T17:30:00Z"));

        assertEquals("PAY-20261109-0007", TopupResponse.from(t).displayId());
    }

    @Test
    @DisplayName("TC-DTO-02 AdminTopupResponse มีรหัสผู้ใช้ U-xxxxxx และ gatewayRef")
    void adminResponse() {
        AdminTopupResponse r = AdminTopupResponse.from(topup(7L, wallet(1L, 4L, "0.00"), 500, Instant.now()));

        assertEquals("U-000004", r.userDisplayId());
        assertEquals("mock_ref_7", r.gatewayRef());
        assertEquals(7L, r.topup().topupId());
    }

    @Test
    @DisplayName("TC-DTO-03 WalletResponse displayId W-xxxxxx")
    void walletDisplayId() {
        assertEquals("W-000018", WalletResponse.from(wallet(18L, 4L, "0.00")).displayId());
    }
}
