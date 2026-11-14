package com.cosync.wallet.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cosync.wallet.service.QrCodeService;

@ExtendWith(MockitoExtension.class)
class MockPaymentGatewayTest {

    @Mock QrCodeService qrCodeService;

    @InjectMocks MockPaymentGateway gateway;

    @Test
    @DisplayName("TC-MPG-01 ได้ ref ขึ้นต้น mock_ ไม่ซ้ำกัน และ QR จาก QrCodeService")
    void createPayment() {
        when(qrCodeService.generate(500)).thenReturn("data:image/png;base64,QQ");

        CreatedPayment a = gateway.createPayment(500, Instant.now());
        CreatedPayment b = gateway.createPayment(500, Instant.now());

        assertTrue(a.gatewayRef().startsWith("mock_"), a.gatewayRef());
        assertNotEquals(a.gatewayRef(), b.gatewayRef());
        assertEquals("data:image/png;base64,QQ", a.qrImage());
    }

    @Test
    @DisplayName("TC-MPG-02 fetchStatus ไม่มีใครให้ถาม → empty เสมอ")
    void fetchStatus_alwaysEmpty() {
        assertTrue(gateway.fetchStatus("mock_abc").isEmpty());
        verifyNoInteractions(qrCodeService);
    }
}
