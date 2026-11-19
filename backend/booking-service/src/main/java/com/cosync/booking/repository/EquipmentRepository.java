package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
}
