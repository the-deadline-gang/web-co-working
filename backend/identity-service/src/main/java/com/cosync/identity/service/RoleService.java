package com.cosync.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.repository.RoleRepository;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)   // ต้องมี เพราะ getPermissionCodes() โหลดแบบ LAZY
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .toList();
    }
}