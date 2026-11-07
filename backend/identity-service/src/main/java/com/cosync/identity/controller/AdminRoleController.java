package com.cosync.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.role.RoleRequest;
import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.security.RequirePermission;
import com.cosync.identity.service.RoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/roles")
public class AdminRoleController {

    private final RoleService roleService;

    public AdminRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @RequirePermission("role.manage")   // ← ป้ายที่ interceptor จะอ่าน
    public List<RoleResponse> listRoles() {
        return roleService.listRoles();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("role.manage")
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/{roleId}")
    @RequirePermission("role.manage")
    public RoleResponse updateRole(@PathVariable Long roleId,
                                @Valid @RequestBody RoleRequest request) {
        return roleService.update(roleId, request);
    }
}
