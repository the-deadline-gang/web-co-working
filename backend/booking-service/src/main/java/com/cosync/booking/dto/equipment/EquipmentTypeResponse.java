package com.cosync.booking.dto.equipment;

import java.math.BigDecimal;

import com.cosync.booking.entity.EquipmentType;

public record EquipmentTypeResponse(
    Long equipmentTypeId,
    String name,
    Boolean isLoanable,
    BigDecimal loanFee ,
    BigDecimal deposit

) {
    public static EquipmentTypeResponse from(EquipmentType e){
        return new EquipmentTypeResponse(e.getId(), e.getName(), e.isLoanable(), e.getLoanFee(), e.getDeposit());
    }
}
