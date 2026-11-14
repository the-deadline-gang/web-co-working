package com.cosync.wallet.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cosync.wallet.dto.webhook.PaymentWebhook;

class PaymentWebhookTest {

    private static PaymentWebhook hook(String eventId, String ref, String status, Integer amount) {
        return new PaymentWebhook(eventId, ref, status, amount, null);
    }

    @Test
    @DisplayName("TC-PWH-01 ช่องครบ status รู้จัก → valid")
    void valid() {
        assertTrue(hook("evt_1", "ref", "successful", 500).isValid());
        assertTrue(hook("evt_1", "ref", "failed", 500).isValid());
        assertTrue(hook("evt_1", "ref", "successful", 500).isSuccessful());
        assertFalse(hook("evt_1", "ref", "failed", 500).isSuccessful());
    }

    @Test
    @DisplayName("TC-PWH-02 ขาดช่อง / ช่องว่าง / status แปลก → invalid")
    void invalid() {
        assertFalse(hook(null, "ref", "successful", 500).isValid());
        assertFalse(hook(" ", "ref", "successful", 500).isValid());
        assertFalse(hook("evt_1", null, "successful", 500).isValid());
        assertFalse(hook("evt_1", "", "successful", 500).isValid());
        assertFalse(hook("evt_1", "ref", "successful", null).isValid());
        assertFalse(hook("evt_1", "ref", "pending", 500).isValid());
        assertFalse(hook("evt_1", "ref", null, 500).isValid());
    }
}
