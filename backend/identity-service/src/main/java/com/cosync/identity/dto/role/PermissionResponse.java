package com.cosync.identity.dto.role;

import com.cosync.identity.entity.Permission;

public record PermissionResponse (String code, String name) {

    
     public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getCode(), permission.getName());
    }
}
