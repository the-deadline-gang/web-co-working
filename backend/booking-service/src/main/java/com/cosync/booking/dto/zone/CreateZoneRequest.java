package com.cosync.booking.dto.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record  CreateZoneRequest(

    @NotBlank(message="กรุณากรอกชื่อของโซน")
    @Size(max = 50, message = "ชื่อโซนยาวได้ไม่เกิน {max} ตัวอักษร")
    String name
) {
    
}
