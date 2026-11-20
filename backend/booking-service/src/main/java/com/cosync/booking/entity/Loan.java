package com.cosync.booking.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.cosync.booking.entity.enumtype.LoanResult;
import com.cosync.booking.entity.enumtype.LoanStatus;

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

@Entity 
@Table(name="loan")
public class Loan {
        
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long id ;
    
    @Column(name = "user_id" , nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)  
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false , length=20)
    private LoanStatus status = LoanStatus.BORROWED;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "condition_before", nullable = false)
    private int conditionBefore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "condition_after")
    private Integer conditionAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "result" , length=10)
    private LoanResult result;

    @Column(name = "fee" ,precision = 12, scale = 2 , nullable=false)
    private BigDecimal fee;

    @Column(name = "deposit" ,precision = 12, scale = 2 , nullable=false)
    private BigDecimal deposit;

    @Column(name = "deposit_refund" ,precision = 12, scale = 2 )
    private BigDecimal depositRefund;


    @Column(name = "photo_url" ,length=500 )
    private String photoUrl;

    @Column(name = "lent_by" ,length=20 , nullable = false)
    private String lentBy;

    @Column(name = "received_by" ,length=20 )
    private String receivedBy;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "overdue_at")
    private Instant overdueAt;

    @Column(name = "adjusted_by",length=20)
    private String adjustedBy;

    @Column(name = "adjusted_at")
    private Instant adjustedAt;

    @Column(name = "adjust_reason",length=200)
    private String adjustReason;


    @CreationTimestamp 
    @Column(name = "created_at" , nullable =  false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected Loan(){}

    public static Loan borrow(Long userId, Equipment equipment, int conditionBefore, String lentBy, Instant now){
        EquipmentType type = equipment.getEquipmentType();
        if (equipment.getResource() != null) {
            throw new IllegalArgumentException("อุปกรณ์ " + equipment.getBarcode() + " เป็นของประจำห้อง ยืมไม่ได้"); 
        }
        if (type.getLoanFee() == null || type.getDeposit() == null) {
            throw new IllegalArgumentException("ประเภท " + type.getName() + " ยังไม่ได้ตั้งค่ายืมและเงินประกัน");
        }
        requireCondition(conditionBefore); 
        Loan loan = new Loan();
        loan.userId = userId;
        loan.equipment = equipment;
        loan.conditionBefore = conditionBefore;
        loan.fee = type.getLoanFee();          
        loan.deposit = type.getDeposit();
        loan.lentBy = lentBy;
        loan.borrowedAt = now;
        return loan;
       
    }

    private static void requireCondition(int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("คะแนนสภาพต้องอยู่ระหว่าง 1–10 แต่ได้ " + score);
        }
    }

    /**
     * เอาไว้เช็ค status ว่าเป็นแบบที่ต้องการไหม
     * @param action กำัลังทำอะไร
     * @param allowed status อะไรบ้าง
     */
    private void requireStatus(String action, LoanStatus... allowed) {
        for (LoanStatus s : allowed) {
            if (status == s) return;          // ตรงกับตัวใดตัวหนึ่ง = ผ่าน
        }
         throw new IllegalStateException(action + " ได้เฉพาะตอน " + Arrays.toString(allowed)  + " แต่ตอนนี้เป็น " + status);
    }

    /**
     * บันทึกเวลาคืนของโดยจะมีการเช็คว่ามีค่า depositRefund ไหม และ
     * จะมีการเช็คว่ามี status เป็น BORROWED หรือ OVERDUE ไหม
     * @param conditionAfter คะแนนสภาพตอนคืน 1–10
     * @param result ผลประเมิน
     * @param depositRefund  เงินคืนเท่าไหร่
     * @param receivedBy พนักงานที่รับคืน
     * @param photoUrl รูปภาพของอุปกรณ์
     * @param now เวลาที่คืน
     */
    public void markReturned(int conditionAfter, LoanResult result, BigDecimal depositRefund, String receivedBy, String photoUrl, Instant now){
        
        requireStatus("markReturned", LoanStatus.BORROWED, LoanStatus.OVERDUE);
        requireCondition(conditionAfter); 

        if (depositRefund.signum() < 0 || depositRefund.compareTo(deposit) > 0) {
            throw new IllegalArgumentException("depositRefund ต้องอยู่ระหว่าง 0 ถึง " + deposit);
        }
        this.conditionAfter = conditionAfter;
        this.result = result;
        this.depositRefund = depositRefund;
        this.receivedBy = receivedBy;
        this.photoUrl =  photoUrl;
        this.returnedAt = now;
        this.status = LoanStatus.RETURNED;
    }

    /**
     * ยืมเกินเวลา โดยจะมีการเช็คว่ามี status เป็น BORROWED ไหม
     * @param now เวลาที่เกิน
     */
    public void markOverdue(Instant now){
        requireStatus("markOverdue", LoanStatus.BORROWED);
        this.overdueAt = now;
        this.status = LoanStatus.OVERDUE;
    }

    /**
     * เกินเวลามาแล้วแต่ก็ยังไม่คืน จะมีการเช็คว่า status เป็น OVERDUE แล้วหรือยัง
     */
    public void markLost(){
        requireStatus("markLost", LoanStatus.OVERDUE);
        this.status = LoanStatus.LOST;
        this.result = LoanResult.LOST;
        this.depositRefund =  new BigDecimal("0");
    }

    /**
     * ผู้จัดการแก้ผลประเมินย้อนหลังของอุปกรณ์ที่ยืมไป โดยจะมีการเช็คว่า status เป็น RETURNED หรือยัง
     * และจะมีการเช็คว่า adjustedBy เขียนไปรึยัง
     * @param newResult  ผลประเมินใหม่
     * @param newRefund  เงินประกันที่คืนทั้งหมดหลังแก้
     * @param by  โดยใคร
     * @param reason เหตุผลอะไร
     * @param now เวลาที่ปรับ
     */    
    public void adjust(LoanResult newResult, BigDecimal newRefund, String by, String reason, Instant now){
        requireStatus("adjust", LoanStatus.RETURNED );
        if ( this.adjustedBy != null) {
            throw new IllegalStateException("รายการนี้เคยเขียนปรับไปแล้ว");
        }
        if (newRefund.compareTo(depositRefund) <= 0 || newRefund.compareTo(deposit) > 0) {
            throw new IllegalArgumentException("newRefund ต้องมากกว่า " + depositRefund + " และไม่เกิน " + deposit);
        }
        this.result = newResult;
        this.depositRefund = newRefund;
        this.adjustedBy = by ; 
        this.adjustedAt = now;
        this.adjustReason = reason;
    }

    // ---------- getter ----------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Equipment getEquipment() { return equipment; }
    public LoanStatus getStatus() { return status; }
    public int getConditionBefore() { return conditionBefore; }
    /** มีค่าเมื่อคืนแล้ว */
    public Integer getConditionAfter() { return conditionAfter; }
    /** มีค่าเมื่อคืนแล้ว */
    public LoanResult getResult() { return result; }
    public BigDecimal getFee() { return fee; }
    public BigDecimal getDeposit() { return deposit; }
    /** มีค่าเมื่อคืนแล้ว (อาจเป็น 0) */
    public BigDecimal getDepositRefund() { return depositRefund; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLentBy() { return lentBy; }
    public String getReceivedBy() { return receivedBy; }
    public Instant getBorrowedAt() { return borrowedAt; }
    public Instant getReturnedAt() { return returnedAt; }
    /** ตอนที่ job ปิดทำการเปลี่ยนเป็น OVERDUE ใช้นับวันก่อนเป็น LOST */
    public Instant getOverdueAt() { return overdueAt; }
    public String getAdjustedBy() { return adjustedBy; }
    public Instant getAdjustedAt() { return adjustedAt; }
    public String getAdjustReason() { return adjustReason; }
}

