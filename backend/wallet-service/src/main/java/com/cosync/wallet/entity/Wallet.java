package com.cosync.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * กระเป๋าเงินของสมาชิก 1 คน
 * ยอดเปลี่ยนได้ทางเดียวคือ credit() / debit() ซึ่งคืนแถว ledger มาให้ save ทุกครั้ง (BR-07)
 * จึงไม่มีทางแก้ยอดโดยไม่มีรายการใน ledger
 */
@Entity
@Table(name = "wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = Money.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
    }

    public Wallet(Long userId) {
        this.userId = userId;
    }

    // ---------- พฤติกรรม ----------

    public boolean hasEnough(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    /** จำนวนที่ยังขาดถ้าจะตัด amount (0 ถ้าพอ) ไว้ใส่ใน details.shortfall */
    public BigDecimal shortfallFor(BigDecimal amount) {
        BigDecimal shortfall = amount.subtract(balance);
        return shortfall.signum() > 0 ? shortfall : Money.ZERO;
    }

    /** เงินเข้า: เพิ่มยอดแล้วคืนแถว ledger ที่ต้อง save */
    public WalletTransaction credit(TransactionType type, BigDecimal amount,
                                    RefType refType, Long refId, String note) {
        requireDirection(type, true);
        BigDecimal value = Money.of(amount);
        this.balance = this.balance.add(value);
        return new WalletTransaction(this, type, value, this.balance, refType, refId, note);
    }

    /**
     * เงินออก: ลดยอดแล้วคืนแถว ledger ที่ต้อง save
     * service ต้องเช็ก hasEnough() และตอบ 422 เองก่อนเรียก ตรงนี้เป็นด่านกันบั๊กเท่านั้น
     */
    public WalletTransaction debit(TransactionType type, BigDecimal amount,
                                   RefType refType, Long refId, String note) {
        requireDirection(type, false);
        BigDecimal value = Money.of(amount);
        if (!hasEnough(value)) {
            throw new IllegalStateException("ยอดไม่พอ ต้องเช็ก hasEnough() ก่อนเรียก debit()");
        }
        this.balance = this.balance.subtract(value);
        return new WalletTransaction(this, type, value.negate(), this.balance, refType, refId, note);
    }

    private static void requireDirection(TransactionType type, boolean credit) {
        if (type.isCredit() != credit) {
            throw new IllegalArgumentException(type + (credit ? " ไม่ใช่รายการเงินเข้า" : " ไม่ใช่รายการเงินออก"));
        }
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
