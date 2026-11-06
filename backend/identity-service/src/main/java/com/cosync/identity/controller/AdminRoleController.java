package com.cosync.identity.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.security.RequirePermission;
import com.cosync.identity.service.RoleService;

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
}
