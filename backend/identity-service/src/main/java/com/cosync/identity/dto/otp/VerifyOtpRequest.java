package com.cosync.identity.dto.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
     
    @NotBlank (message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    String email,

    @NotBlank(message = "กรุณากรอกรหัส OTP")
    @Pattern(regexp = "^[0-9]{6}$", message = "รหัส OTP ต้องเป็นตัวเลข 6 หลัก")
    String otp
)
{}
