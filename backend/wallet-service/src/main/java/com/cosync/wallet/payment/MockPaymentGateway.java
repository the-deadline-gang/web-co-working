package com.cosync.wallet.payment;

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
    public CreatedPayment createPayment(int amountThb) {
        String ref = "mock_" + UUID.randomUUID();          // ref ปลอม เอาไปใส่ใน webhook ของ Bruno
        return new CreatedPayment(ref, qrCodeService.generate(amountThb));
    }
}