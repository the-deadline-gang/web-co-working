package com.cosync.wallet.payment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.cosync.wallet.service.QrCodeService;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private final QrCodeService qrCodeService;

    public MockPaymentGateway(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @Override
    public CreatedPayment createPayment(int amountThb, Instant expiresAt) {
        String ref = "mock_" + UUID.randomUUID();
        return new CreatedPayment(ref, qrCodeService.generate(amountThb));   // QR ของเราเองไม่มีวันหมดอายุ เลยไม่ได้ใช้ expiresAt
    }

    @Override
    public Optional<PaymentResult> fetchStatus(String gatewayRef) {
        return Optional.empty();       // mock ไม่มีใครให้ถาม ผลการจ่ายมาทาง /webhooks/payment อย่างเดียว
    }
}