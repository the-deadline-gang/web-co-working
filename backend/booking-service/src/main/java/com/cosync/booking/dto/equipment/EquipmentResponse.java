package com.cosync.booking.dto.equipment;

import java.math.BigDecimal;

import com.cosync.booking.entity.Equipment;
import com.cosync.booking.entity.enumtype.EquipmentStatus;

public record EquipmentResponse(  
    Long equipmentId, 
    String barcode, 
    String typeName, 
    Long equipmentTypeId,
    EquipmentStatus status, 
    String resourceCode, 
    BigDecimal loanFee, 
    BigDecimal deposit
) { 
    public static EquipmentResponse from(Equipment e){
       return new EquipmentResponse(e.getId(), e.getBarcode(),e.getEquipmentType().getName() , e.getEquipmentType().getId(), e.getStatus(), e.getResource() == null ? null : e.getResource().getCode().trim(), e.getEquipmentType().getLoanFee(), e.getEquipmentType().getDeposit());
    } 
}