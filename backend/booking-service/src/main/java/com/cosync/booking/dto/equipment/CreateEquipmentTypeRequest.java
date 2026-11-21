package com.cosync.booking.dto.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEquipmentTypeRequest(

    @NotBlank(message="กรอกชื่อ")
    @Size(max = 100)
    String name,

    @NotNull(message="กรอกค่า isLoanable")
    Boolean isLoanable

) {
}
