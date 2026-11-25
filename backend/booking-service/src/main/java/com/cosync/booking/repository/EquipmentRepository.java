package com.cosync.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.Equipment;
import com.cosync.booking.entity.Resource;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByResource(Resource resource);
}
