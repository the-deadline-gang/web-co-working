package com.cosync.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** รายการเติมเงินผ่าน PromptPay QR (F3) */
@Entity
@Table(name = "topup")
public class Topup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topup_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** ซ้ำกับใน wallet ไว้เช็กเจ้าของได้โดยไม่ต้องโหลด wallet (ENG-6) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_thb", nullable = false)
    private Integer amountThb;

    /** rate ตอนสร้าง เปลี่ยนค่าตั้งทีหลังไม่กระทบรายการนี้ (BR-02) */
    @Column(name = "rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    @Column(name = "token_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal tokenAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TopupStatus status = TopupStatus.PENDING;

    @Column(name = "gateway_ref", nullable = false, unique = true, length = 100)
    private String gatewayRef;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** BR-17: เติมจากหน้าจองที่ยอดไม่พอ */
    @Column(name = "return_to_booking_id")
    private Long returnToBookingId;

    /** รายการ ledger ที่เติมเงินจริง มีค่าเมื่อ PAID */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txn_id", unique = true)
    private WalletTransaction transaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Topup() {
    }

    public Topup(Wallet wallet, int amountThb, BigDecimal rate, String gatewayRef,
                 Instant expiresAt, Long returnToBookingId) {
        this.wallet = wallet;
        this.userId = wallet.getUserId();
        this.amountThb = amountThb;
        this.rate = rate;
        this.tokenAmount = Money.tokensFor(amountThb, rate);
        this.gatewayRef = gatewayRef;
        this.expiresAt = expiresAt;
        this.returnToBookingId = returnToBookingId;
    }

    // ---------- พฤติกรรม ----------

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean isPending() {
        return status == TopupStatus.PENDING;
    }

    /** PENDING ที่เลยเวลาแล้ว (job ใช้หา, GET ใช้เช็กก่อนตอบ) */
    public boolean isOverdue(Instant now) {
        return isPending() && now.isAfter(expiresAt);
    }

    /**
     * เงินเข้าแล้ว: เติม Token เข้า wallet แล้วคืนแถว ledger ที่ต้อง save
     * TODO Q1: ตอนนี้รับเฉพาะ PENDING ถ้าตกลงว่าจ่ายช้าก็เติมให้ ให้เพิ่ม EXPIRED ตรงนี้
     */
    public WalletTransaction markPaid(Instant paidAt) {
        if (!isPending()) {
            throw new IllegalStateException("topup " + id + " ไม่ได้อยู่ในสถานะ PENDING แต่เป็น " + status);
        }
        WalletTransaction txn = wallet.credit(TransactionType.TOPUP, tokenAmount, RefType.TOPUP, id,
                "เติมเงิน " + amountThb + " บาท");
        this.transaction = txn;
        this.paidAt = paidAt;
        this.status = TopupStatus.PAID;
        return txn;
    }

    public void markExpired() {
        if (isPending()) {
            this.status = TopupStatus.EXPIRED;
        }
    }

    public void markFailed() {
        if (isPending()) {
            this.status = TopupStatus.FAILED;
        }
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public Wallet getWallet() { return wallet; }
    public Long getUserId() { return userId; }
    public Integer getAmountThb() { return amountThb; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getTokenAmount() { return tokenAmount; }
    public TopupStatus getStatus() { return status; }
    public String getGatewayRef() { return gatewayRef; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public Long getReturnToBookingId() { return returnToBookingId; }
    public WalletTransaction getTransaction() { return transaction; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
