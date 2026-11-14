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
// review 10/11/2026 by peak   เดี๋ยวจะเอาออกถ้าเราจะใช้ gateway จากข้่างนอกสร้าง qr-code
@Service
public class QrCodeService {

    private final String promptPayPhone;

    public QrCodeService(@Value("${app.payment.promptpay-id}") String promptPayPhone) {
        this.promptPayPhone = promptPayPhone;
    }

    /** คืนรูป QR เป็น data URL ใส่ <img src> ได้เลย */
    /**
     * เอาไว้ gen Qr-code  
     * @param amountThb จำนวนเงิน
     * @return 
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