package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.BookingSetting;

public interface BookingSettingRepository extends JpaRepository<BookingSetting, Integer> {
    
}
