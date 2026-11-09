package com.cosync.identity.client;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cosync.identity.common.WalletUnavailableException;

/** เรียก wallet-service ผ่าน /internal (ไม่ผ่าน Gateway) */
@Component
public class WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);

    private final RestClient wallet;

    public WalletClient(@Value("${app.wallet.url}") String baseUrl,
                        @Value("${app.internal.token}") String internalToken) {
        // ตั้ง timeout ไว้ ถ้า wallet ค้าง จะได้ไม่รอไปเรื่อยๆ จนหน้าเว็บหมุนค้าง
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(3));

        this.wallet = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeouts)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    /** สร้าง wallet ให้ user (เรียกซ้ำได้) ไม่สำเร็จด้วยเหตุใดก็ตาม → WalletUnavailableException */
    public void createWallet(Long userId) {
        try {
            wallet.post().uri("/internal/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("userId", userId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("สร้าง wallet ให้ user {} ไม่สำเร็จ", userId, e);
            throw new WalletUnavailableException();
        }
    }
}
