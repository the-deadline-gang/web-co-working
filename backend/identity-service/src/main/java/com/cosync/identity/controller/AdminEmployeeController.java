package com.cosync.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.employee.CreateEmployeeRequest;
import com.cosync.identity.dto.employee.EmployeeResponse;
import com.cosync.identity.dto.employee.UpdateEmployeeRequest;
import com.cosync.identity.entity.EmployeeStatus;
import com.cosync.identity.security.RequirePermission;
import com.cosync.identity.service.EmployeeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    public AdminEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("employee.manage")
    public EmployeeResponse createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping 
    @RequirePermission("employee.manage")
    public List<EmployeeResponse> listEmployees(
            @RequestParam(required = false) EmployeeStatus status) {
        return employeeService.list(status);
    }


    @PutMapping("/{employeeId}")
    @RequirePermission("employee.manage")
    public EmployeeResponse updateEmployee(
            @PathVariable Long employeeId,
            @RequestHeader("X-User-Id") Long currentUserId,    // คนที่กำลังแก้
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(employeeId, currentUserId, request);
    }
}