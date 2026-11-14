package com.cosync.wallet.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * webhook ทุกตัวที่ได้รับ ใช้ event_id เป็น PK กันเติมซ้ำ (BR-05)
 *
 * ทำไม implements Persistable: id เป็นค่าที่ Gateway ให้มา ไม่ได้ให้ DB สร้าง
 * ถ้าไม่บอก Spring Data ว่าเป็นของใหม่ save() จะใช้ merge (SELECT แล้ว UPDATE ทับ)
 * event ซ้ำจะไม่ error และเราจะไม่รู้ว่าซ้ำ พอบอกว่า isNew() = true
 * save() จะ INSERT เสมอ event ซ้ำจึงชน PK แล้วได้ DataIntegrityViolationException
 */
@Entity
@Table(name = "payment_event")
public class PaymentEvent implements Persistable<String> {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "gateway_ref", nullable = false, length = 100)
    private String gatewayRef;

    /** ค่าจาก Gateway: successful / failed */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "amount_thb", nullable = false)
    private Integer amountThb;

    /** body ดิบที่ Gateway ส่งมา เก็บไว้เป็นหลักฐานตอนกระทบยอด */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected PaymentEvent() {
    }

    public PaymentEvent(String eventId, String gatewayRef, String status, int amountThb, String payload) {
        this.eventId = eventId;
        this.gatewayRef = gatewayRef;
        this.status = status;
        this.amountThb = amountThb;
        this.payload = payload;
    }

    public boolean isSuccessful() {
        return "successful".equals(status);
    }

    // ---------- Persistable ----------

    @Override
    public String getId() {
        return eventId;
    }

    /** ยังไม่เคยบันทึก = receivedAt ยังว่าง (@CreationTimestamp ใส่ให้ตอน insert) */
    @Override
    public boolean isNew() {
        return receivedAt == null;
    }

    // ---------- getter ----------

    public String getEventId() { return eventId; }
    public String getGatewayRef() { return gatewayRef; }
    public String getStatus() { return status; }
    public Integer getAmountThb() { return amountThb; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
}
