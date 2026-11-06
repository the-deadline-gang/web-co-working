package com.cosync.identity.dto.employee;

import com.cosync.identity.entity.Employee;

/** ข้อมูลพนักงานที่ส่งให้หน้าหลังบ้าน (ตาม schema Employee) */
public record EmployeeResponse(
        Long employeeId,
        String displayId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Long roleId,
        String roleName,
        String status) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                "STF-%03d".formatted(employee.getId()),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole().getId(),
                employee.getRole().getName(),
                employee.getStatus().name());   // enum → "ACTIVE"
    }
}