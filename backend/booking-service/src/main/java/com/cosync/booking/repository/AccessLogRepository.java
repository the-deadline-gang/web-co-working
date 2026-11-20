package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.AccessLog;

public interface AccessLogRepository  extends JpaRepository<AccessLog, Long>{
    
}
