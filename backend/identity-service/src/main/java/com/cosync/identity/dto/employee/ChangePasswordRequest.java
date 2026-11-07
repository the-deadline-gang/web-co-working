package com.cosync.identity.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "กรุณากรอกรหัสผ่านปัจจุบัน")
        String currentPassword,

        @NotBlank(message = "กรุณากรอกรหัสผ่านใหม่")
        @Size(min = 8, max = 72, message = "รหัสผ่านใหม่ต้องยาว 8-72 ตัวอักษร")
        String newPassword) {
}