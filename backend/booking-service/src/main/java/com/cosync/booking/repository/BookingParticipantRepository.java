package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.BookingParticipant;
import com.cosync.booking.entity.BookingParticipantId;

public interface BookingParticipantRepository  extends  JpaRepository<BookingParticipant, BookingParticipantId>{
    
}
