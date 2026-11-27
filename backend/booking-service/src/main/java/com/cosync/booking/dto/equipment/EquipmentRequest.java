package com.cosync.booking.dto.equipment;


import com.cosync.booking.entity.enumtype.EquipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record  EquipmentRequest (

    @NotNull(message="กรอก equipmentTypeId ด้วย")
    Long equipmentTypeId,

    @NotBlank(message="กรอก barcode ด้วย")
    @Size(max = 30)
    String barcode,

    Long resourceId,
    
    EquipmentStatus status

)
{

}
