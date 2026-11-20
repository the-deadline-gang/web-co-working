package com.cosync.booking.dto.setting;

import java.time.Instant;
import java.time.LocalTime;

import com.cosync.booking.entity.BookingSetting;

public record BookingSettingsResponse (

    LocalTime openTime,
    LocalTime closeTime,
    int bookAheadDays,
    int holdMinutes,
    int holdExtendMinutes,
    int earlyCheckinMinutes,
    int noShowMinutes,
    int cancelFullRefundHours,
    int cancelHalfRefundHours,
    int loanMaxItems,
    int lostAfterDays,
    String updatedBy,
    Instant updatedAt
){
    public static BookingSettingsResponse from(BookingSetting s ){
        return  new BookingSettingsResponse(
            s.getOpenTime(),
            s.getCloseTime(),
            s.getBookAheadDays(),
            s.getHoldMinutes(),
            s.getHoldExtendMinutes(),
            s.getEarlyCheckinMinutes(),
            s.getNoShowMinutes(),
            s.getCancelFullRefundHours(),
            s.getCancelHalfRefundHours(),
            s.getLoanMaxItems(),
            s.getLostAfterDays(),
            s.getUpdatedBy(),
            s.getUpdatedAt()
             
     );
   }
}
