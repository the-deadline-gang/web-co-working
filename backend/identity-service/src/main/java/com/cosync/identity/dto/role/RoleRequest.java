package com.cosync.identity.dto.role;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleRequest(

    @NotBlank(message = "กรุณากรอกชื่อ role")
    @Size(max = 60, message = "ชื่อ role ยาวได้ไม่เกิน 60 ตัวอักษร")
    String name,

    @NotNull(message = "กรุณาระบุสิทธิ์ (ส่ง [] ได้ถ้าไม่มี)")
    List<String> permissions) {
}
