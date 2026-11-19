package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.Resource;


public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
}
