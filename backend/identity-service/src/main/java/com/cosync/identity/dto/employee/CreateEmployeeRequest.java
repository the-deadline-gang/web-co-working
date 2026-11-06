package com.cosync.identity.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(

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

      
        @Pattern(regexp = "^0[0-9]{9}$", message = "เบอร์โทรต้องเป็นตัวเลข 10 หลักขึ้นต้นด้วย 0")
        String phone,

        @NotNull(message = "กรุณาเลือกบทบาท")
        Long roleId

) {}
