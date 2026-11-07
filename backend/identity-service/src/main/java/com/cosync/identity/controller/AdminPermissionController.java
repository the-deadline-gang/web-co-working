package com.cosync.identity.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.role.PermissionResponse;
import com.cosync.identity.security.RequirePermission;
import com.cosync.identity.service.RoleService;

@RestController
@RequestMapping("/admin/permissions")
public class AdminPermissionController {

    private final RoleService roleService;

    public AdminPermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @RequirePermission("role.manage")
    public List<PermissionResponse> listPermissions() {
        return roleService.listPermissions();
    }
}