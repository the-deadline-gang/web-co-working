package com.cosync.booking.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.cosync.booking.dto.setting.BookingSettingsRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "booking_setting")
public class BookingSetting {
    
    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "setting_id" , nullable=false)
    private int settingId;

    @Column(name = "open_time" , nullable=false)
    private LocalTime openTime;

    @Column(name = "close_time" , nullable=false)
    private LocalTime closeTime;

    @Column(name = "book_ahead_days" , nullable=false)
    private int bookAheadDays;

    @Column(name = "hold_minutes" , nullable=false)
    private int holdMinutes;

    @Column(name = "hold_extend_minutes" , nullable=false)
    private int holdExtendMinutes;

    @Column(name = "early_checkin_minutes" , nullable=false)
    private int earlyCheckinMinutes;

    @Column(name = "no_show_minutes" , nullable=false)
    private int noShowMinutes;

    @Column(name = "cancel_full_refund_hours" , nullable=false)
    private int cancelFullRefundHours;

    @Column(name = "cancel_half_refund_hours" , nullable=false)
    private int cancelHalfRefundHours;

    @Column(name = "loan_max_items" , nullable=false)
    private int loanMaxItems;

    @Column(name = "lost_after_days" , nullable = false )
    private int lostAfterDays;

    @Column(name = "last_end_of_day" )
    private LocalDate lastEndOfDay;

    @Column(name = "updated_by" , length = 20)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at" , nullable=false )
    private Instant updatedAt;

    protected BookingSetting(){}
  
    public void update(BookingSettingsRequest req, String updatedBy){

        if (!req.openTime().isBefore(req.closeTime())) {
            throw new IllegalArgumentException("openTime ต้องมาก่อน closeTime");
        }
        if (req.cancelHalfRefundHours() >= req.cancelFullRefundHours()) {
            throw new IllegalArgumentException("cancelHalfRefundHours ต้องน้อยกว่า cancelFullRefundHours");
        }
        this.openTime = req.openTime();
        this.closeTime = req.closeTime();
        this.bookAheadDays = req.bookAheadDays();
        this.holdMinutes = req.holdMinutes();
        this.holdExtendMinutes = req.holdExtendMinutes();
        this.earlyCheckinMinutes = req.earlyCheckinMinutes();
        this.noShowMinutes = req.noShowMinutes();
        this.cancelFullRefundHours = req.cancelFullRefundHours();
        this.cancelHalfRefundHours = req.cancelHalfRefundHours();
        this.loanMaxItems = req.loanMaxItems();
        this.lostAfterDays = req.lostAfterDays();
        this.updatedBy = updatedBy;
    }

    public void markEndOfDayDone(LocalDate day){
        this.lastEndOfDay = day;
    }

    // ---------- getter ----------

    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public int getBookAheadDays() { return bookAheadDays; }
    public int getHoldMinutes() { return holdMinutes; }
    public int getHoldExtendMinutes() { return holdExtendMinutes; }
    public int getEarlyCheckinMinutes() { return earlyCheckinMinutes; }
    public int getNoShowMinutes() { return noShowMinutes; }
    public int getCancelFullRefundHours() { return cancelFullRefundHours; }
    public int getCancelHalfRefundHours() { return cancelHalfRefundHours; }
    public int getLoanMaxItems() { return loanMaxItems; }
    public int getLostAfterDays() { return lostAfterDays; }
    public LocalDate getLastEndOfDay() { return lastEndOfDay; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
