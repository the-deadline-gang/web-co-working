package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.ServiceRate;
import com.cosync.booking.entity.enumtype.ServiceType;

public interface ServiceRateRepository extends JpaRepository<ServiceRate, ServiceType> {
    
}
