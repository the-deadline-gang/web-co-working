package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    
}
