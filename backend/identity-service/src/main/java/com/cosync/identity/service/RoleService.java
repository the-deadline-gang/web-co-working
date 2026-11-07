package com.cosync.identity.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.dto.role.PermissionResponse;
import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.repository.PermissionRepository;
import com.cosync.identity.repository.RoleRepository;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository,PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)   // ต้องมี เพราะ getPermissionCodes() โหลดแบบ LAZY
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .toList();
    }

  @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll(Sort.by("id")).stream()
                .map(PermissionResponse::from)
                .toList();
    }
}