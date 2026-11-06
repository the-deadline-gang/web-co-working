package com.cosync.identity.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeInputRequest(

    @NotBlank(message = "กรุณากรอกชืิ่อ")
    @Size(max = 60, message = "ชื่อยาวได้ไม่เกิน 60 ตัวอักษร")
    String firstName,

    @NotBlank(message = "กรุณากรอกนามสกุล")
    @Size(max = 60, message = "นามสกุลยาวได้ไม่เกิน 60 ตัวอักษร")
    String lastName,

    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 120, message = "อีเมลยาวได้ไม่เกิน 120 ตัวอักษร")
    String email,

    @NotNull(message = "กรุณาเลือกบทบาท")
    Long roleId,

   
    @Pattern(regexp = "^0[0-9]{9}$", message = "รหัส OTP ต้องเป็นตัวเลข 6 หลัก")
    String phone
   
) {}


/*  
        firstName: { type: string, maxLength: 60 }
        lastName: { type: string, maxLength: 60 }
        email: { type: string, format: email }
        phone: { type: string, pattern: '^0[0-9]{9}$' }
        roleId: { type: integer, format: int64 }
         */