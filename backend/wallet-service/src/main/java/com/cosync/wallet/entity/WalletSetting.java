package com.cosync.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** ค่าตั้งของฝ่ายการเงิน มีแถวเดียว setting_id = 1 (สร้างใน V2) */
@Entity
@Table(name = "wallet_setting")
public class WalletSetting {

    public static final int SINGLETON_ID = 1;

    /** ปุ่มยอดที่หน้าเติมเงินแสดง ตัดตัวที่อยู่นอกช่วง min ถึง max ออก */
    private static final List<Integer> QUICK_AMOUNTS = List.of(100, 200, 500, 1000, 2000);

    @Id
    @Column(name = "setting_id")
    private Integer id;

    /** 1 บาท ได้กี่ Token (BR-02) */
    @Column(name = "rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    @Column(name = "min_thb", nullable = false)
    private Integer minThb;

    @Column(name = "max_thb", nullable = false)
    private Integer maxThb;

    @Column(name = "qr_expire_minutes", nullable = false)
    private Integer qrExpireMinutes;

    /** เช่น STF-004 */
    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletSetting() {
    }

    // ---------- พฤติกรรม ----------

    /** BR-01 */
    public boolean allowsAmount(int amountThb) {
        return amountThb >= minThb && amountThb <= maxThb;
    }

    public List<Integer> quickAmounts() {
        return QUICK_AMOUNTS.stream().filter(this::allowsAmount).toList();
    }

    /** service ต้องเช็ก min <= max และตอบ 400 เองก่อน ตรงนี้เป็นด่านกันบั๊ก (DB ก็มี CHECK) */
    public void update(BigDecimal rate, int minThb, int maxThb, int qrExpireMinutes, String updatedBy) {
        if (minThb > maxThb) {
            throw new IllegalArgumentException("minThb ต้องไม่มากกว่า maxThb");
        }
        this.rate = rate;
        this.minThb = minThb;
        this.maxThb = maxThb;
        this.qrExpireMinutes = qrExpireMinutes;
        this.updatedBy = updatedBy;
    }

    // ---------- getter ----------

    public Integer getId() { return id; }
    public BigDecimal getRate() { return rate; }
    public Integer getMinThb() { return minThb; }
    public Integer getMaxThb() { return maxThb; }
    public Integer getQrExpireMinutes() { return qrExpireMinutes; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
