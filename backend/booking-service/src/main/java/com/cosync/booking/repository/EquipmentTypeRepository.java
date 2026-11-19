package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.EquipmentType;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {
    
}
