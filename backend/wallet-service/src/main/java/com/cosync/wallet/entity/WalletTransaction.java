package com.cosync.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

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
import jakarta.persistence.Table;

/**
 * แถวใน ledger: การเปลี่ยนยอด 1 ครั้ง (BR-07)
 * สร้างได้จาก Wallet.credit() / Wallet.debit() เท่านั้น
 * @Immutable = Hibernate ไม่ UPDATE แถวนี้หลัง insert (ENG-3)
 */
@Entity
@Immutable
@Table(name = "wallet_transaction")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    /** บวก = เงินเข้า, ลบ = เงินออก */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 20)
    private RefType refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletTransaction() {
    }

    /** package-private: ให้ Wallet เป็นคนสร้างเท่านั้น */
    WalletTransaction(Wallet wallet, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
                      RefType refType, Long refId, String note) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.refType = refType;
        this.refId = refId;
        this.note = note;
    }

    /**
     * ผูก Idempotency-Key กับรายการนี้ (ENG-4) เรียกได้ครั้งเดียวก่อน save
     * รายการที่มาจาก webhook เติมเงินไม่ต้องมี key
     */
    public WalletTransaction withIdempotency(String idempotencyKey, String requestHash) {
        if (this.id != null || this.idempotencyKey != null) {
            throw new IllegalStateException("ตั้ง Idempotency-Key ได้ครั้งเดียวก่อน save");
        }
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        return this;
    }

    /** body เดิมหรือไม่ (key เดิม + hash เดิม = request ซ้ำ ตอบผลเดิมได้) */
    public boolean matchesRequest(String requestHash) {
        return this.requestHash != null && this.requestHash.equals(requestHash);
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public Wallet getWallet() { return wallet; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public RefType getRefType() { return refType; }
    public Long getRefId() { return refId; }
    public String getNote() { return note; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
}
