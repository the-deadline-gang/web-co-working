package com.cosync.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    @Test
    @DisplayName("TC-QR-01 ได้ data URL ของรูป PNG ขนาด 300×300")
    void generatesPng() throws Exception {
        String url = new QrCodeService("0812345678").generate(500);

        assertTrue(url.startsWith("data:image/png;base64,"));
        byte[] png = Base64.getDecoder().decode(url.substring("data:image/png;base64,".length()));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
    }
}
