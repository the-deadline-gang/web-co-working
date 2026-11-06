package com.cosync.identity.dto.login;

import java.util.Set;

import com.cosync.identity.entity.Employee;

/** ข้อมูลพนักงานที่ส่งให้ frontend (ตาม schema StaffMe) */
public record StaffMeResponse(
        Long employeeId,
        String displayId,
        String firstName,
        String role,
        Set<String> permissions) {

    public static StaffMeResponse from(Employee employee) {
        return new StaffMeResponse(
                employee.getId(),
                "STF-%03d".formatted(employee.getId()),   
                employee.getFirstName(),
                employee.getRole().getName(),
                employee.getRole().getPermissionCodes());
    }
}