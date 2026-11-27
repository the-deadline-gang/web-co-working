package com.cosync.booking.dto.equipment;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record  EquipmentPricingRequest (

    @NotNull(message="กรอก loanFee ด้วย")
    @DecimalMin("0") 
    @Digits(integer = 10, fraction = 2)
    BigDecimal loanFee,

    @NotNull(message="กรอก deposit ด้วย")
    @DecimalMin("0") 
    @Digits(integer = 10, fraction = 2)
    BigDecimal deposit
){
    
}
