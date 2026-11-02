package com.cosync.identity.dto.register;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "กรุณากรอกชื่อ")
        @Size(max = 60, message = "ชื่อยาวได้ไม่เกิน 60 ตัวอักษร")
        String firstName,

        @NotBlank(message = "กรุณากรอกนามสกุล")
        @Size(max = 60, message = "นามสกุลยาวได้ไม่เกิน 60 ตัวอักษร")
        String lastName,

        @NotBlank(message = "กรุณากรอกอีเมล")
        @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
        @Size(max = 120, message = "อีเมลยาวได้ไม่เกิน 120 ตัวอักษร")
        String email,

        @NotBlank(message = "กรุณากรอกเบอร์โทร")
        @Pattern(regexp = "^0[0-9]{9}$", message = "เบอร์โทรต้องเป็นตัวเลข 10 หลักขึ้นต้นด้วย 0")
        String phone,

        @NotBlank(message = "กรุณากรอกรหัสผ่าน")
        @Size(min = 8, max = 72, message = "รหัสผ่านต้องยาว 8 ถึง 72 ตัวอักษร")
        String password,

        @AssertTrue(message = "ต้องยอมรับเงื่อนไขการใช้งานและนโยบายความเป็นส่วนตัว")
        boolean acceptTerms
) {
}