package com.cosync.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.setting.BookingSettingsRequest;
import com.cosync.booking.dto.setting.BookingSettingsResponse;
import com.cosync.booking.entity.BookingSetting;
import com.cosync.booking.repository.BookingSettingRepository;

@Service
public class SettingService {
    
    private final BookingSettingRepository  bookingSettingRepository;
    public SettingService(BookingSettingRepository  bookingSettingRepository){
        this.bookingSettingRepository=  bookingSettingRepository;
    }

    /**
     * ดึงการตั้งค่าของ booksetting 
     * @return ส่งกลับแบบ BookingSettingsResponse
     */
    public BookingSettingsResponse getSettings(){
        BookingSetting setting = bookingSettingRepository.findById(BookingSetting.SINGLETON_ID).orElseThrow();
        return  BookingSettingsResponse.from(setting);
    }

    /**
     * เอาไว้ update booksetting โดย method นี้เป็นแบบ Transaction 
     * จะมีการเช็คการว่าเวลาเปิดกับปิด นั้นเวลาเปิดต้องก่อนเวลาปิด รึป่าว
     * มีการเช็คว่า cancelHalfRefundHours มากกว่าหรือเท่ากับ cancelFullRefundHours รึป่าว
     * @param staffId id ของพนักงาน
     * @param req คำขอที่เป็นใน format BookingSettingsRequest
     * @return ส่งกลับแบบ BookingSettingsResponse
     */
    @Transactional
    public  BookingSettingsResponse updateSettings(Long staffId, BookingSettingsRequest req){
        if ( !req.openTime().isBefore(req.closeTime()) ) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED","เวลาเปิดต้องก่อนเวลาปิด");
        }
        if(req.cancelHalfRefundHours() >= req.cancelFullRefundHours() ){
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED","cancelHalfRefundHours ต้องน้อยกว่า cancelFullRefundHours");
        }
        BookingSetting setting = bookingSettingRepository.findById(BookingSetting.SINGLETON_ID).orElseThrow(() -> new IllegalStateException("ไม่มีแถว booking_setting (ลืมรัน V2?)"));

        setting.update(req, "STF-%03d".formatted(staffId));
        bookingSettingRepository.flush();          // ส่ง UPDATE ทันที @UpdateTimestamp จะใส่เวลาใหม่ให้

        return BookingSettingsResponse.from(setting);
    }
}
