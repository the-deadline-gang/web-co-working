package com.cosync.wallet.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cosync.wallet.qr.PromptPayPayload;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * สร้าง QR PromptPay ของเราเอง ใช้กับ MockPaymentGateway (โหมดไม่มีเน็ต)
 * โหมด Omise ไม่ได้ใช้คลาสนี้ เพราะ QR มาจาก Omise
 */
@Service
public class QrCodeService {

    private final String promptPayPhone;

    public QrCodeService(@Value("${app.payment.promptpay-id}") String promptPayPhone) {
        this.promptPayPhone = promptPayPhone;
    }

    /**
     * สร้าง QR PromptPay ตามยอดเงิน
     * @param amountThb ยอดเงิน (บาท)
     * @return รูป QR เป็น data URL ใส่ใน img src ได้เลย
     */
    public String generate(int amountThb) {
        String payload = PromptPayPayload.build(promptPayPhone, BigDecimal.valueOf(amountThb));
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("สร้าง QR ไม่ได้", e);
        }
    }
}
