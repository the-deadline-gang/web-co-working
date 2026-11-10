package com.cosync.wallet.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cosync.wallet.common.ApiException;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "omise")
public class OmisePaymentGateway implements PaymentGateway {

    private final RestClient omise;

    public OmisePaymentGateway(@Value("${app.payment.omise.secret-key}") String secretKey) {
        this.omise = RestClient.builder()
                .baseUrl("https://api.omise.co")
                .defaultHeaders(h -> h.setBasicAuth(secretKey, ""))      // = curl -u "$KEY:"
                .build();
    }

    
    @Override
    public CreatedPayment createPayment(int amountThb) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", String.valueOf(amountThb * 100));             // บาท → สตางค์
        form.add("currency", "THB");
        form.add("source[type]", "promptpay");

        try {
            OmiseCharge charge = omise.post().uri("/charges")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OmiseCharge.class);
            return new CreatedPayment(charge.id(), charge.qrUrl());
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_UNAVAILABLE",
                    "ระบบชำระเงินขัดข้อง กรุณาลองใหม่");
        }
    }
}