package com.cosync.identity.dto.role;

import java.util.Set;

import com.cosync.identity.entity.Role;

public record RoleResponse(Long roleId, String name, Set<String> permissions) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getPermissionCodes());
    }
}