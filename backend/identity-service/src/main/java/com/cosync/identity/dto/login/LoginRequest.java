package com.cosync.identity.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record  LoginRequest (
    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 120, message = "อีเมลยาวได้ไม่เกิน 120 ตัวอักษร")
    String email,

    @NotBlank(message = "กรุณากรอกรหัสผ่าน")
    @Size(max = 72, message = "รหัสผ่านยาวเกินไป")
    String password
)
{}
